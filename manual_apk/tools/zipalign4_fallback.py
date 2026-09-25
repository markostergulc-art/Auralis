#!/usr/bin/env python3
"""Repack a ZIP/APK so each STORED entry starts at a 4-byte boundary.
This preserves uncompressed entry content (and therefore JAR/V1 digests) but is
only a fallback for environments without Android SDK zipalign.
"""
import argparse, io, struct, zipfile
from pathlib import Path

def clone_info(src):
    z=zipfile.ZipInfo(src.filename, src.date_time)
    z.compress_type=src.compress_type
    z.comment=src.comment
    z.extra=src.extra or b''
    z.internal_attr=src.internal_attr
    z.external_attr=src.external_attr
    z.create_system=src.create_system
    z.create_version=src.create_version
    z.extract_version=src.extract_version
    return z

def align_extra(base_extra: bytes, pos: int, name_len: int):
    base_data=pos+30+name_len+len(base_extra)
    rem=base_data%4
    if rem==0: return base_extra
    needed=(-rem)%4
    # Valid private ZIP extra record: 2-byte id + 2-byte payload length + payload.
    # Header is 4 bytes so only payload affects modulo 4.
    rec=struct.pack('<HH',0xCAFE,needed)+b'\0'*needed
    return base_extra+rec

def main():
    a=argparse.ArgumentParser();a.add_argument('src');a.add_argument('dst');x=a.parse_args()
    src=Path(x.src);dst=Path(x.dst)
    with zipfile.ZipFile(src,'r') as zin, open(dst,'wb') as raw:
        with zipfile.ZipFile(raw,'w',allowZip64=False) as zout:
            zout.comment=zin.comment
            for info in zin.infolist():
                data=zin.read(info.filename)
                z=clone_info(info)
                if info.compress_type==zipfile.ZIP_STORED:
                    name_bytes=info.filename.encode('utf-8')
                    z.extra=align_extra(z.extra, raw.tell(), len(name_bytes))
                zout.writestr(z,data,compress_type=info.compress_type,compresslevel=6)
    # Assert all STORED entries aligned.
    b=dst.read_bytes()
    with zipfile.ZipFile(dst) as z:
        for i in z.infolist():
            if i.compress_type!=zipfile.ZIP_STORED: continue
            off=i.header_offset; fn=struct.unpack_from('<H',b,off+26)[0]; ex=struct.unpack_from('<H',b,off+28)[0]
            data_off=off+30+fn+ex
            if data_off%4: raise SystemExit(f'unaligned STORED entry {i.filename}: {data_off}')
    print(dst)
if __name__=='__main__': main()
