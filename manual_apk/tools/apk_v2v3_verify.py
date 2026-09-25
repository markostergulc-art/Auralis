#!/usr/bin/env python3
from __future__ import annotations
import argparse,hashlib,struct,zipfile
from pathlib import Path
from cryptography import x509
from cryptography.hazmat.primitives import hashes
from cryptography.hazmat.primitives.asymmetric import padding
EOCD=b'PK\x05\x06';MAGIC=b'APK Sig Block 42';V2=0x7109871A;V3=0xF05368C0

def lp(buf,p): n=struct.unpack_from('<I',buf,p)[0]; return buf[p+4:p+4+n],p+4+n
def u32(n): return struct.pack('<I',n)
def calc(b,st,cd,e):
 eo=bytearray(b[e:]); struct.pack_into('<I',eo,16,st); chunks=[]
 for sec in (b[:st],b[cd:e],bytes(eo)):
  for o in range(0,len(sec),1024*1024):
   ch=sec[o:o+1024*1024]; chunks.append(hashlib.sha256(b'\xa5'+u32(len(ch))+ch).digest())
 return hashlib.sha256(b'\x5a'+u32(len(chunks))+b''.join(chunks)).digest()
def main():
 a=argparse.ArgumentParser();a.add_argument('apk');x=a.parse_args();b=Path(x.apk).read_bytes();e=b.rfind(EOCD);cd=struct.unpack_from('<I',b,e+16)[0]
 if b[cd-16:cd]!=MAGIC: raise SystemExit('FAIL no APK Signing Block')
 sz=struct.unpack_from('<Q',b,cd-24)[0];st=cd-sz-8; dg=calc(b,st,cd,e); p=st+8; end=cd-24; ok={}
 while p<end:
  ln=struct.unpack_from('<Q',b,p)[0]; pid=struct.unpack_from('<I',b,p+8)[0]; val=b[p+12:p+8+ln]; p+=8+ln
  if pid not in (V2,V3): continue
  signers,_=lp(val,0); signer,_=lp(signers,0); pos=0; signed,pos=lp(signer,pos)
  if pid==V3: mino,maxo=struct.unpack_from('<II',signer,pos); pos+=8
  sigs,pos=lp(signer,pos); pub,pos=lp(signer,pos)
  recs,_=lp(sigs,0); alg=struct.unpack_from('<I',recs,0)[0]; sig,_=lp(recs,4)
  sp=0; digs,sp=lp(signed,sp); certs,sp=lp(signed,sp); dr,_=lp(digs,0); dalg=struct.unpack_from('<I',dr,0)[0]; got,_=lp(dr,4); certder,_=lp(certs,0)
  if pid==V3: mins,maxs=struct.unpack_from('<II',signed,sp); sp+=8
  attrs,sp=lp(signed,sp)
  cert=x509.load_der_x509_certificate(certder); cert.public_key().verify(sig,signed,padding.PKCS1v15(),hashes.SHA256())
  ok[pid]=(got==dg and alg==0x0103 and dalg==0x0103,hashlib.sha256(certder).hexdigest())
 print('V2',ok.get(V2)); print('V3',ok.get(V3)); print('content_digest',dg.hex())
 if not (ok.get(V2,(False,))[0] and ok.get(V3,(False,))[0]): raise SystemExit(1)
 # ZIP parser integrity check
 with zipfile.ZipFile(x.apk) as z: bad=z.testzip(); print('zip_integrity', 'PASS' if bad is None else 'FAIL '+bad)
if __name__=='__main__':main()
