#!/usr/bin/env python3
"""Minimal APK Signature Scheme v2/v3 signer for an already V1-signed APK.
Uses a PKCS#12 RSA key. Intended as an offline fallback when Android apksigner
is unavailable. Production release pipelines should still prefer SDK apksigner.
"""
from __future__ import annotations
import argparse, hashlib, struct
from pathlib import Path
from cryptography.hazmat.primitives.serialization import pkcs12, Encoding, PublicFormat
from cryptography.hazmat.primitives.asymmetric import padding, rsa
from cryptography.hazmat.primitives import hashes

EOCD_SIG=b'PK\x05\x06'; MAGIC=b'APK Sig Block 42'
ID_V2=0x7109871A; ID_V3=0xF05368C0; ALG_RSA_PKCS1_SHA256=0x0103
STRIPPING_PROTECTION_ATTR_ID=0xBEEFF00D

def u32(n): return struct.pack('<I',n)
def u64(n): return struct.pack('<Q',n)
def lp(b: bytes): return u32(len(b))+b
def seq(items): return lp(b''.join(lp(x) for x in items))

def locate(apk: bytes):
    e=apk.rfind(EOCD_SIG)
    if e<0: raise ValueError('EOCD not found')
    if e+22>len(apk): raise ValueError('truncated EOCD')
    comment_len=struct.unpack_from('<H',apk,e+20)[0]
    if e+22+comment_len!=len(apk): raise ValueError('EOCD/comment length mismatch')
    cd=struct.unpack_from('<I',apk,e+16)[0]
    return cd,e

def content_digest(apk_without_sigblock: bytes)->bytes:
    cd,e=locate(apk_without_sigblock)
    eocd=bytearray(apk_without_sigblock[e:])
    struct.pack_into('<I',eocd,16,cd)  # offset of signing block == original CD offset
    sections=[apk_without_sigblock[:cd],apk_without_sigblock[cd:e],bytes(eocd)]
    cds=[]
    for sec in sections:
        for off in range(0,len(sec),1024*1024):
            ch=sec[off:off+1024*1024]
            cds.append(hashlib.sha256(b'\xa5'+u32(len(ch))+ch).digest())
    return hashlib.sha256(b'\x5a'+u32(len(cds))+b''.join(cds)).digest()

def sign_record(key, signed_data: bytes)->bytes:
    signature=key.sign(signed_data,padding.PKCS1v15(),hashes.SHA256())
    rec=u32(ALG_RSA_PKCS1_SHA256)+lp(signature)
    return seq([rec])

def digest_records(digest: bytes)->bytes:
    return seq([u32(ALG_RSA_PKCS1_SHA256)+lp(digest)])

def cert_records(cert_der: bytes)->bytes:
    return seq([cert_der])

def build_v2(key, cert_der, pub_der, digest):
    # v2 stripping-protection attr indicates a v3 signer is also present.
    attr=u32(STRIPPING_PROTECTION_ATTR_ID)+u32(3)
    attrs=seq([attr])
    signed_data=digest_records(digest)+cert_records(cert_der)+attrs
    signer=lp(signed_data)+sign_record(key,signed_data)+lp(pub_der)
    return seq([signer])

def build_v3(key, cert_der, pub_der, digest):
    min_sdk=28; max_sdk=0x7fffffff
    attrs=seq([])
    signed_data=digest_records(digest)+cert_records(cert_der)+u32(min_sdk)+u32(max_sdk)+attrs
    signer=lp(signed_data)+u32(min_sdk)+u32(max_sdk)+sign_record(key,signed_data)+lp(pub_der)
    return seq([signer])

def build_block(v2:bytes,v3:bytes)->bytes:
    pairs=b''
    for pid,val in ((ID_V2,v2),(ID_V3,v3)):
        pair=u32(pid)+val
        pairs+=u64(len(pair))+pair
    # size excludes first uint64 but includes ending size + magic
    size=len(pairs)+8+len(MAGIC)
    return u64(size)+pairs+u64(size)+MAGIC

def insert_block(apk:bytes, block:bytes)->bytes:
    cd,e=locate(apk)
    out=bytearray(apk[:cd]+block+apk[cd:])
    new_e=e+len(block)
    struct.pack_into('<I',out,new_e+16,cd+len(block))
    return bytes(out)

def main():
    ap=argparse.ArgumentParser(); ap.add_argument('input'); ap.add_argument('output'); ap.add_argument('--p12',required=True); ap.add_argument('--password',required=True)
    a=ap.parse_args()
    src=Path(a.input).read_bytes()
    key,cert,chain=pkcs12.load_key_and_certificates(Path(a.p12).read_bytes(),a.password.encode())
    if not isinstance(key,rsa.RSAPrivateKey): raise SystemExit('RSA PKCS#12 key required')
    if cert is None: raise SystemExit('certificate missing')
    cert_der=cert.public_bytes(Encoding.DER); pub_der=cert.public_key().public_bytes(Encoding.DER,PublicFormat.SubjectPublicKeyInfo)
    dg=content_digest(src)
    block=build_block(build_v2(key,cert_der,pub_der,dg),build_v3(key,cert_der,pub_der,dg))
    Path(a.output).write_bytes(insert_block(src,block))
    print('digest',dg.hex()); print('cert_sha256',hashlib.sha256(cert_der).hexdigest()); print('block_bytes',len(block))
if __name__=='__main__': main()
