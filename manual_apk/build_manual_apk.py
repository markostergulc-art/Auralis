import os, struct, hashlib, zlib, zipfile, shutil
from pathlib import Path

ROOT=Path(__file__).resolve().parent
OUT=ROOT/'out'
shutil.rmtree(OUT, ignore_errors=True); OUT.mkdir(parents=True)
NO_INDEX=0xffffffff

def uleb(n:int)->bytes:
    out=bytearray()
    while True:
        b=n&0x7f; n>>=7
        if n: out.append(b|0x80)
        else: out.append(b); break
    return bytes(out)
def sleb(n:int)->bytes:
    out=bytearray(); more=True
    while more:
        b=n&0x7f; n>>=7; sign=b&0x40
        more=not ((n==0 and not sign) or (n==-1 and sign))
        if more:b|=0x80
        out.append(b)
    return bytes(out)
def align4(buf):
    while len(buf)%4: buf.append(0)
def p16(x): return struct.pack('<H',x&0xffff)
def p32(x): return struct.pack('<I',x&0xffffffff)

# -------- DEX --------
MAIN='Lcom/marko/auralis/MainActivity;'
ACT='Landroid/app/Activity;'; BUNDLE='Landroid/os/Bundle;'; CONTEXT='Landroid/content/Context;'
WEBVIEW='Landroid/webkit/WebView;'; WEBSET='Landroid/webkit/WebSettings;'; VIEW='Landroid/view/View;'
STRING='Ljava/lang/String;'; OBJECT='Ljava/lang/Object;'; VOID='V'; BOOL='Z'; INT='I'; FLOAT='F'; LONG='J'
BYTEARR='[B'; BASE64='Landroid/util/Base64;'; AUDIOTRACK='Landroid/media/AudioTrack;'; AUDIODEVICE='Landroid/media/AudioDeviceInfo;'
PROCESS='Landroid/os/Process;'; DEBUG='Landroid/os/Debug;'; JSANN='Landroid/webkit/JavascriptInterface;'; THROWABLE='Ljava/lang/Throwable;'

P_VOID0=(VOID,()); P_VOID_BUNDLE=(VOID,(BUNDLE,)); P_VOID_VIEW=(VOID,(VIEW,)); P_VOID_CONTEXT=(VOID,(CONTEXT,))
P_WEBSET0=(WEBSET,()); P_VOID_BOOL=(VOID,(BOOL,)); P_VOID_STRING=(VOID,(STRING,)); P_VOID_INT=(VOID,(INT,))
P_VIEW_INT=(VIEW,(INT,)); P_BOOL0=(BOOL,()); P_VOID_OBJ_STRING=(VOID,(OBJECT,STRING))
P_AUDIOTRACK_6I=(VOID,(INT,INT,INT,INT,INT,INT)); P_INT_I=(INT,(INT,)); P_INT_3I=(INT,(INT,INT,INT)); P_INT_F=(INT,(FLOAT,)); P_INT_BII=(INT,(BYTEARR,INT,INT))
P_BYTES_SI=(BYTEARR,(STRING,INT)); P_BOOL_SIII=(BOOL,(STRING,INT,INT,INT)); P_VOID_F=(VOID,(FLOAT,)); P_INT0=(INT,()); P_LONG0=(LONG,()); P_AUDIODEVICE0=(AUDIODEVICE,())

methods=[
 (ACT,'<init>',P_VOID0),(ACT,'onCreate',P_VOID_BUNDLE),(ACT,'onBackPressed',P_VOID0),(ACT,'onDestroy',P_VOID0),(ACT,'setContentView',P_VOID_VIEW),(ACT,'findViewById',P_VIEW_INT),(ACT,'finish',P_VOID0),
 (WEBVIEW,'<init>',P_VOID_CONTEXT),(WEBVIEW,'getSettings',P_WEBSET0),(WEBVIEW,'loadUrl',P_VOID_STRING),(WEBVIEW,'addJavascriptInterface',P_VOID_OBJ_STRING),
 (VIEW,'setId',P_VOID_INT),
 (WEBSET,'setJavaScriptEnabled',P_VOID_BOOL),(WEBSET,'setDomStorageEnabled',P_VOID_BOOL),(WEBSET,'setAllowFileAccessFromFileURLs',P_VOID_BOOL),
 (AUDIOTRACK,'<init>',P_AUDIOTRACK_6I),(AUDIOTRACK,'getNativeOutputSampleRate',P_INT_I),(AUDIOTRACK,'play',P_VOID0),(AUDIOTRACK,'stop',P_VOID0),(AUDIOTRACK,'release',P_VOID0),(AUDIOTRACK,'write',P_INT_BII),(AUDIOTRACK,'setLoopPoints',P_INT_3I),(AUDIOTRACK,'setVolume',P_INT_F),(AUDIOTRACK,'getRoutedDevice',P_AUDIODEVICE0),(AUDIODEVICE,'getType',P_INT0),
 (BASE64,'decode',P_BYTES_SI),(PROCESS,'getElapsedCpuTime',P_LONG0),(DEBUG,'getPss',P_INT0),
 (MAIN,'<init>',P_VOID0),(MAIN,'onCreate',P_VOID_BUNDLE),(MAIN,'onBackPressed',P_VOID0),(MAIN,'onDestroy',P_VOID0),
 (MAIN,'startPcm',P_BOOL_SIII),(MAIN,'stopPcm',P_VOID0),(MAIN,'setPcmVolume',P_VOID_F),(MAIN,'getNativeSampleRate',P_INT0),(MAIN,'getCpuTimeMs',P_LONG0),(MAIN,'getMemoryPssKb',P_INT0),(MAIN,'getOutputDeviceType',P_INT0),(MAIN,'exitApp',P_VOID0),
]
fields=[(MAIN,'audioTrack',AUDIOTRACK)]
protos=sorted(set(m[2] for m in methods),key=lambda p:(p[0],p[1]))
types={MAIN,ACT,BUNDLE,CONTEXT,WEBVIEW,WEBSET,VIEW,STRING,OBJECT,VOID,BOOL,INT,FLOAT,LONG,BYTEARR,BASE64,AUDIOTRACK,AUDIODEVICE,PROCESS,DEBUG,JSANN,THROWABLE}
for r,ps in protos: types.add(r); types.update(ps)
for c,n,t in fields: types.add(c); types.add(t)
def shorty(proto):
    r,ps=proto
    ss=lambda t:t[0] if len(t)==1 else 'L'
    return ss(r)+''.join(ss(x) for x in ps)
strings=set(types)
for c,n,p in methods: strings.add(n); strings.add(shorty(p))
for c,n,t in fields: strings.add(n)
for lit in ['file:///android_asset/index.html','Android','javascript:handleAndroidBack()']:
    strings.add(lit)
strings=sorted(strings); str_idx={s:i for i,s in enumerate(strings)}
types=sorted(types,key=lambda t:str_idx[t]); type_idx={t:i for i,t in enumerate(types)}
protos=sorted(protos,key=lambda p:(type_idx[p[0]],tuple(type_idx[x] for x in p[1]))); proto_idx={p:i for i,p in enumerate(protos)}
fields=sorted(fields,key=lambda f:(type_idx[f[0]],str_idx[f[1]],type_idx[f[2]])); field_idx={f:i for i,f in enumerate(fields)}
methods=sorted(methods,key=lambda m:(type_idx[m[0]],str_idx[m[1]],proto_idx[m[2]])); method_idx={m:i for i,m in enumerate(methods)}

header_size=0x70; string_ids_off=header_size; string_ids_size=len(strings)
type_ids_off=string_ids_off+4*string_ids_size; type_ids_size=len(types)
proto_ids_off=type_ids_off+4*type_ids_size; proto_ids_size=len(protos)
field_ids_off=proto_ids_off+12*proto_ids_size; field_ids_size=len(fields)
method_ids_off=field_ids_off+8*field_ids_size; method_ids_size=len(methods)
class_defs_off=method_ids_off+8*method_ids_size; data_off=class_defs_off+32
data=bytearray(); abs_off=lambda:data_off+len(data)
param_off={}
for ps in sorted(set(p[1] for p in protos if p[1]),key=lambda ps:tuple(type_idx[x] for x in ps)):
    align4(data);param_off[ps]=abs_off();data+=p32(len(ps))
    for t in ps:data+=p16(type_idx[t])
    if len(ps)%2:data+=p16(0)
string_data_offs={};first_string_data=None
for st in strings:
    if first_string_data is None:first_string_data=abs_off()
    string_data_offs[st]=abs_off();enc=st.encode();data+=uleb(len(st))+enc+b'\0'

# instruction helpers
def invoke(op,midx,regs):
    A=len(regs);rr=list(regs)+[0]*(5-len(regs));C,D,E,F,G=rr[:5]
    return [op|(G<<8)|(A<<12),midx,C|(D<<4)|(E<<8)|(F<<12)]
def invoke_range(op,midx,start,count): return [op|(count<<8),midx,start]
def newinst(d,t):return [0x22|(d<<8),t]
def moveresobj(d):return [0x0c|(d<<8)]
def moveres(d):return [0x0a|(d<<8)]
def movereswide(d):return [0x0b|(d<<8)]
def moveexc(d):return [0x0d|(d<<8)]
def move(d,s):return [0x01|(d<<8)|(s<<12)]
def movefrom16(d,s):return [0x02|(d<<8),s]
def const4(d,lit):return [0x12|(d<<8)|((lit&0xf)<<12)]
def const16(d,lit):return [0x13|(d<<8),lit&0xffff]
def conststr(d,si):return [0x1a|(d<<8),si]
def checkcast(r,t):return [0x1f|(r<<8),t]
def arraylen(a,b):return [0x21|(a<<8)|(b<<12)]
def igetobj(a,b,f):return [0x54|(a<<8)|(b<<12),f]
def iputobj(a,b,f):return [0x5b|(a<<8)|(b<<12),f]
def retvoid():return [0x0e]
def ret(r):return [0x0f|(r<<8)]
def retwide(r):return [0x10|(r<<8)]

class Asm:
    def __init__(self):self.u=[];self.labels={};self.fix=[]
    def emit(self,units):self.u+=units
    def label(self,n):self.labels[n]=len(self.u)
    def ifz(self,r,label):
        pos=len(self.u);self.u+=[0x38|(r<<8),0];self.fix.append((pos+1,pos,label))
    def patch(self):
        for slot,start,label in self.fix:
            off=self.labels[label]-start
            if not -32768<=off<=32767:raise ValueError((label,off))
            self.u[slot]=off&0xffff
        return self.u

def code_item(regs,ins_sz,outs,units,try_count=0,try_start=0,try_end=0,catch_addr=0):
    b=bytearray(struct.pack('<HHHHII',regs,ins_sz,outs,try_count,0,len(units)))
    for x in units:b+=p16(x)
    if try_count:
        if len(units)%2:b+=p16(0)
        b+=struct.pack('<IHH',try_start,try_end-try_start,1)
        b+=uleb(1)+sleb(0)+uleb(catch_addr)
    return b

align4(data); first_code_off=abs_off(); code_offs={}; code_count=0
# ctor
ctor=(MAIN,'<init>',P_VOID0);code_offs[ctor]=abs_off();units=invoke(0x70,method_idx[(ACT,'<init>',P_VOID0)],[0])+retvoid();data+=code_item(1,1,1,units);align4(data);code_count+=1
# onCreate p0=v3,p1=v4
oncreate=(MAIN,'onCreate',P_VOID_BUNDLE);code_offs[oncreate]=abs_off();units=[]
units+=invoke(0x6f,method_idx[(ACT,'onCreate',P_VOID_BUNDLE)],[3,4])
units+=newinst(0,type_idx[WEBVIEW]);units+=invoke(0x70,method_idx[(WEBVIEW,'<init>',P_VOID_CONTEXT)],[0,3])
units+=const4(2,7);units+=invoke(0x6e,method_idx[(VIEW,'setId',P_VOID_INT)],[0,2])
units+=invoke(0x6e,method_idx[(WEBVIEW,'getSettings',P_WEBSET0)],[0]);units+=moveresobj(1);units+=const4(2,1)
for name in ['setJavaScriptEnabled','setDomStorageEnabled','setAllowFileAccessFromFileURLs']:
    units+=invoke(0x6e,method_idx[(WEBSET,name,P_VOID_BOOL)],[1,2])
units+=conststr(2,str_idx['Android']);units+=invoke(0x6e,method_idx[(WEBVIEW,'addJavascriptInterface',P_VOID_OBJ_STRING)],[0,3,2])
units+=invoke(0x6e,method_idx[(ACT,'setContentView',P_VOID_VIEW)],[3,0]);units+=conststr(2,str_idx['file:///android_asset/index.html']);units+=invoke(0x6e,method_idx[(WEBVIEW,'loadUrl',P_VOID_STRING)],[0,2]);units+=retvoid()
data+=code_item(5,2,3,units);align4(data);code_count+=1
# onBackPressed p0=v2
onback=(MAIN,'onBackPressed',P_VOID0);code_offs[onback]=abs_off();units=const4(1,7)+invoke(0x6e,method_idx[(ACT,'findViewById',P_VIEW_INT)],[2,1])+moveresobj(0)+checkcast(0,type_idx[WEBVIEW])+conststr(1,str_idx['javascript:handleAndroidBack()'])+invoke(0x6e,method_idx[(WEBVIEW,'loadUrl',P_VOID_STRING)],[0,1])+retvoid();data+=code_item(3,1,2,units);align4(data);code_count+=1
# onDestroy p0=v0
ondestroy=(MAIN,'onDestroy',P_VOID0);code_offs[ondestroy]=abs_off();units=invoke(0x6e,method_idx[(MAIN,'stopPcm',P_VOID0)],[0])+invoke(0x6f,method_idx[(ACT,'onDestroy',P_VOID0)],[0])+retvoid();data+=code_item(1,1,1,units);align4(data);code_count+=1
# stopPcm p0=v2
stop=(MAIN,'stopPcm',P_VOID0);code_offs[stop]=abs_off();a=Asm();a.emit(igetobj(0,2,field_idx[(MAIN,'audioTrack',AUDIOTRACK)]));a.ifz(0,'done');a.emit(invoke(0x6e,method_idx[(AUDIOTRACK,'stop',P_VOID0)],[0]));a.emit(invoke(0x6e,method_idx[(AUDIOTRACK,'release',P_VOID0)],[0]));a.emit(const4(1,0));a.emit(iputobj(1,2,field_idx[(MAIN,'audioTrack',AUDIOTRACK)]));a.label('done');a.emit(retvoid());data+=code_item(3,1,1,a.patch());align4(data);code_count+=1
# setPcmVolume p0=v1,p1=v2
setvol=(MAIN,'setPcmVolume',P_VOID_F);code_offs[setvol]=abs_off();a=Asm();a.emit(igetobj(0,1,field_idx[(MAIN,'audioTrack',AUDIOTRACK)]));a.ifz(0,'done');a.emit(invoke(0x6e,method_idx[(AUDIOTRACK,'setVolume',P_INT_F)],[0,2]));a.label('done');a.emit(retvoid());data+=code_item(3,2,2,a.patch());align4(data);code_count+=1
# getNativeSampleRate p0=v1
getrate=(MAIN,'getNativeSampleRate',P_INT0);code_offs[getrate]=abs_off();units=const4(0,3)+invoke(0x71,method_idx[(AUDIOTRACK,'getNativeOutputSampleRate',P_INT_I)],[0])+moveres(0)+ret(0);data+=code_item(2,1,1,units);align4(data);code_count+=1
# getCpuTimeMs p0=v2, v0/v1 wide
getcpu=(MAIN,'getCpuTimeMs',P_LONG0);code_offs[getcpu]=abs_off();units=invoke(0x71,method_idx[(PROCESS,'getElapsedCpuTime',P_LONG0)],[])+movereswide(0)+retwide(0);data+=code_item(3,1,0,units);align4(data);code_count+=1
# getMemoryPssKb p0=v1
getmem=(MAIN,'getMemoryPssKb',P_INT0);code_offs[getmem]=abs_off();units=invoke(0x71,method_idx[(DEBUG,'getPss',P_INT0)],[])+moveres(0)+ret(0);data+=code_item(2,1,0,units);align4(data);code_count+=1
# getOutputDeviceType p0=v1; returns Android AudioDeviceInfo.TYPE_* or 0 when unresolved
getroute=(MAIN,'getOutputDeviceType',P_INT0);code_offs[getroute]=abs_off();a=Asm();a.emit(igetobj(0,1,field_idx[(MAIN,'audioTrack',AUDIOTRACK)]));a.ifz(0,'none');a.emit(invoke(0x6e,method_idx[(AUDIOTRACK,'getRoutedDevice',P_AUDIODEVICE0)],[0]));a.emit(moveresobj(0));a.ifz(0,'none');a.emit(invoke(0x6e,method_idx[(AUDIODEVICE,'getType',P_INT0)],[0]));a.emit(moveres(0));a.emit(ret(0));a.label('none');a.emit(const4(0,0));a.emit(ret(0));data+=code_item(2,1,1,a.patch());align4(data);code_count+=1
# exitApp p0=v0
exitapp=(MAIN,'exitApp',P_VOID0);code_offs[exitapp]=abs_off();units=invoke(0x6e,method_idx[(ACT,'finish',P_VOID0)],[0])+retvoid();data+=code_item(1,1,1,units);align4(data);code_count+=1
# startPcm p0=v11 p1=v12 p2=v13 p3=v14 p4=v15
start=(MAIN,'startPcm',P_BOOL_SIII);code_offs[start]=abs_off();a=Asm();a.emit(invoke(0x6e,method_idx[(MAIN,'stopPcm',P_VOID0)],[11]));a.emit(const4(0,0));a.emit(invoke(0x71,method_idx[(BASE64,'decode',P_BYTES_SI)],[12,0]));a.emit(moveresobj(1));a.emit(arraylen(2,1));a.emit(newinst(3,type_idx[AUDIOTRACK]));a.emit(const4(4,3));a.emit(movefrom16(5,13));a.emit(const16(6,12));a.emit(const4(7,2));a.emit(move(8,2));a.emit(const4(9,0));a.emit(invoke_range(0x76,method_idx[(AUDIOTRACK,'<init>',P_AUDIOTRACK_6I)],3,7));a.emit(const4(0,0));a.emit(invoke(0x6e,method_idx[(AUDIOTRACK,'write',P_INT_BII)],[3,1,0,2]));a.emit(const4(0,0));a.emit(invoke(0x6e,method_idx[(AUDIOTRACK,'setLoopPoints',P_INT_3I)],[3,0,14,15]));a.emit(iputobj(3,11,field_idx[(MAIN,'audioTrack',AUDIOTRACK)]));a.emit(invoke(0x6e,method_idx[(AUDIOTRACK,'play',P_VOID0)],[3]));a.emit(const4(0,1));a.emit(ret(0));a.label('catch');a.emit(moveexc(10));a.emit(const4(0,0));a.emit(ret(0));units=a.patch();catch_addr=a.labels['catch'];data+=code_item(16,5,7,units,1,0,catch_addr,catch_addr);align4(data);code_count+=1

# class data: 0 static fields, 1 instance field, 1 direct, 11 virtual
class_data_off=abs_off();cd=bytearray();cd+=uleb(0)+uleb(1)+uleb(1)+uleb(11);cd+=uleb(field_idx[(MAIN,'audioTrack',AUDIOTRACK)])+uleb(0x2);cd+=uleb(method_idx[ctor])+uleb(0x10001)+uleb(code_offs[ctor])
virtual_specs=[(oncreate,0x4),(onback,0x1),(ondestroy,0x4),(start,0x1),(stop,0x1),(setvol,0x1),(getrate,0x1),(getcpu,0x1),(getmem,0x1),(getroute,0x1),(exitapp,0x1)]
virtuals=sorted([(method_idx[m],m,flags) for m,flags in virtual_specs])
prev=0
for idx,m,flags in virtuals:cd+=uleb(idx-prev)+uleb(flags)+uleb(code_offs[m]);prev=idx
data+=cd
# @JavascriptInterface on bridge methods
annotation_item_off=abs_off();data+=bytes([1])+uleb(type_idx[JSANN])+uleb(0)
align4(data);annotation_set_off=abs_off();data+=p32(1)+p32(annotation_item_off)
align4(data);annotations_dir_off=abs_off();bridge=[start,stop,setvol,getrate,getcpu,getmem,getroute,exitapp];entries=sorted((method_idx[m],annotation_set_off) for m in bridge);data+=p32(0)+p32(0)+p32(len(entries))+p32(0)
for midx,aoff in entries:data+=p32(midx)+p32(aoff)
align4(data)
# map
map_off=abs_off();maps=[]
def amap(t,size,off):
    if size:maps.append((off,t,size))
amap(0x0000,1,0);amap(0x0001,string_ids_size,string_ids_off);amap(0x0002,type_ids_size,type_ids_off);amap(0x0003,proto_ids_size,proto_ids_off);amap(0x0004,field_ids_size,field_ids_off);amap(0x0005,method_ids_size,method_ids_off);amap(0x0006,1,class_defs_off)
if param_off:amap(0x1001,len(param_off),min(param_off.values()))
amap(0x2002,string_ids_size,first_string_data);amap(0x2001,code_count,first_code_off);amap(0x2000,1,class_data_off);amap(0x2004,1,annotation_item_off);amap(0x1003,1,annotation_set_off);amap(0x2006,1,annotations_dir_off);amap(0x1000,1,map_off);maps.sort();data+=p32(len(maps))
for off,t,size in maps:data+=struct.pack('<HHII',t,0,size,off)
file_size=data_off+len(data);buf=bytearray(file_size)
pos=string_ids_off
for st in strings:struct.pack_into('<I',buf,pos,string_data_offs[st]);pos+=4
pos=type_ids_off
for t in types:struct.pack_into('<I',buf,pos,str_idx[t]);pos+=4
pos=proto_ids_off
for p in protos:
    r,ps=p;struct.pack_into('<III',buf,pos,str_idx[shorty(p)],type_idx[r],param_off.get(ps,0));pos+=12
pos=field_ids_off
for c,n,t in fields:struct.pack_into('<HHI',buf,pos,type_idx[c],type_idx[t],str_idx[n]);pos+=8
pos=method_ids_off
for m in methods:
    c,n,p=m;struct.pack_into('<HHI',buf,pos,type_idx[c],proto_idx[p],str_idx[n]);pos+=8
struct.pack_into('<IIIIIIII',buf,class_defs_off,type_idx[MAIN],0x1,type_idx[ACT],0,NO_INDEX,annotations_dir_off,class_data_off,0);buf[data_off:]=data
buf[0:8]=b'dex\n035\0';struct.pack_into('<I',buf,32,file_size);struct.pack_into('<I',buf,36,0x70);struct.pack_into('<I',buf,40,0x12345678);struct.pack_into('<I',buf,52,map_off);struct.pack_into('<II',buf,56,string_ids_size,string_ids_off);struct.pack_into('<II',buf,64,type_ids_size,type_ids_off);struct.pack_into('<II',buf,72,proto_ids_size,proto_ids_off);struct.pack_into('<II',buf,80,field_ids_size,field_ids_off);struct.pack_into('<II',buf,88,method_ids_size,method_ids_off);struct.pack_into('<II',buf,96,1,class_defs_off);struct.pack_into('<II',buf,104,len(data),data_off);buf[12:32]=hashlib.sha1(buf[32:]).digest();struct.pack_into('<I',buf,8,zlib.adler32(buf[12:])&0xffffffff);(OUT/'classes.dex').write_bytes(buf)

# -------- resources + manifest --------
RES_STRING_POOL_TYPE=0x0001;UTF8_FLAG=0x100
def enc_len8(n):return bytes([(n>>8)|0x80,n&0xff]) if n>0x7f else bytes([n])
def string_pool(vals):
    offsets=[];body=bytearray()
    for st in vals:
        offsets.append(len(body));b=st.encode();body+=enc_len8(len(st))+enc_len8(len(b))+b+b'\0'
    while len(body)%4:body.append(0)
    hs=28;start=hs+4*len(vals);total=start+len(body);o=bytearray(struct.pack('<HHI',RES_STRING_POOL_TYPE,hs,total));o+=struct.pack('<IIIII',len(vals),0,UTF8_FLAG,start,0)
    for x in offsets:o+=p32(x)
    o+=body;return bytes(o)
RES_TABLE_TYPE=0x0002;RES_TABLE_PACKAGE_TYPE=0x0200;RES_TABLE_TYPE_TYPE=0x0201;RES_TABLE_TYPE_SPEC_TYPE=0x0202
value_pool=string_pool(['res/drawable/app_icon.png']);type_pool=string_pool(['drawable']);key_pool=string_pool(['app_icon'])
spec=bytearray(struct.pack('<HHI',RES_TABLE_TYPE_SPEC_TYPE,16,20));spec+=struct.pack('<BBHI',1,0,0,1);spec+=p32(0)
config_size=64;config=p32(config_size)+b'\0'*(config_size-4);type_header_size=20+config_size;entries_start=type_header_size+4;type_size=entries_start+16
typechunk=bytearray(struct.pack('<HHI',RES_TABLE_TYPE_TYPE,type_header_size,type_size));typechunk+=struct.pack('<BBHII',1,0,0,1,entries_start);typechunk+=config;typechunk+=p32(0);typechunk+=struct.pack('<HHI',8,0,0);typechunk+=struct.pack('<HBBI',8,0,0x03,0)
pkg_header_size=288;type_off=pkg_header_size;key_off=type_off+len(type_pool);pkg_size=pkg_header_size+len(type_pool)+len(key_pool)+len(spec)+len(typechunk)
pkg=bytearray(struct.pack('<HHI',RES_TABLE_PACKAGE_TYPE,pkg_header_size,pkg_size));pkg+=p32(0x7f);name='com.marko.auralis'.encode('utf-16le')+b'\0\0';pkg+=name+b'\0'*(256-len(name));pkg+=p32(type_off)+p32(0)+p32(key_off)+p32(0)+p32(0);pkg+=type_pool+key_pool+spec+typechunk
arsc=bytearray(struct.pack('<HHI',RES_TABLE_TYPE,12,12+len(value_pool)+len(pkg)));arsc+=p32(1)+value_pool+pkg;(OUT/'resources.arsc').write_bytes(arsc)

RES_XML_TYPE=0x0003;RES_XML_RESOURCE_MAP_TYPE=0x0180;RES_XML_START_NAMESPACE_TYPE=0x0100;RES_XML_END_NAMESPACE_TYPE=0x0101;RES_XML_START_ELEMENT_TYPE=0x0102;RES_XML_END_ELEMENT_TYPE=0x0103
TYPE_REFERENCE=0x01;TYPE_STRING=0x03;TYPE_INT_DEC=0x10;TYPE_INT_BOOLEAN=0x12
attr_ids={'theme':0x01010000,'versionCode':0x0101021b,'versionName':0x0101021c,'minSdkVersion':0x0101020c,'targetSdkVersion':0x01010270,'label':0x01010001,'icon':0x01010002,'name':0x01010003,'exported':0x01010010}
sp=list(attr_ids)+['android','http://schemas.android.com/apk/res/android','manifest','uses-sdk','application','activity','intent-filter','action','category','package','com.marko.auralis','2.0.1','Auralis','com.marko.auralis.MainActivity','android.intent.action.MAIN','android.intent.category.LAUNCHER']
seen=set();sp=[x for x in sp if not(x in seen or seen.add(x))];si={x:i for i,x in enumerate(sp)};AURI=si['http://schemas.android.com/apk/res/android'];APFX=si['android']
def nh(t,size,line=1,comment=NO_INDEX):return struct.pack('<HHIII',t,16,size,line,comment)
def sns(p,u):return nh(RES_XML_START_NAMESPACE_TYPE,24)+p32(p)+p32(u)
def ens(p,u):return nh(RES_XML_END_NAMESPACE_TYPE,24)+p32(p)+p32(u)
def attr(ns,name,raw=None,dtype=TYPE_STRING,data_val=None):
    nsi=NO_INDEX if ns is None else ns;ni=si[name]
    if dtype==TYPE_STRING:ri=si[raw];data=ri if data_val is None else data_val
    else:ri=NO_INDEX if raw is None else si[raw];data=0 if data_val is None else data_val
    return struct.pack('<IIIHBBI',nsi,ni,ri,8,0,dtype,data)
def sel(name,attrs,ns=NO_INDEX):
    ab=b''.join(attrs);size=36+len(ab);return nh(RES_XML_START_ELEMENT_TYPE,size)+struct.pack('<IIHHHHHH',ns,si[name],20,20,len(attrs),0,0,0)+ab
def eel(name,ns=NO_INDEX):return nh(RES_XML_END_ELEMENT_TYPE,24)+p32(ns)+p32(si[name])
xml=bytearray();xml+=string_pool(sp);ids=[attr_ids[n] for n in attr_ids];xml+=struct.pack('<HHI',RES_XML_RESOURCE_MAP_TYPE,8,8+4*len(ids))+b''.join(p32(x) for x in ids);xml+=sns(APFX,AURI);xml+=sel('manifest',[attr(None,'package','com.marko.auralis'),attr(AURI,'versionCode',dtype=TYPE_INT_DEC,data_val=21),attr(AURI,'versionName','2.0.1')]);xml+=sel('uses-sdk',[attr(AURI,'minSdkVersion',dtype=TYPE_INT_DEC,data_val=26),attr(AURI,'targetSdkVersion',dtype=TYPE_INT_DEC,data_val=36)]);xml+=eel('uses-sdk');xml+=sel('application',[attr(AURI,'label','Auralis'),attr(AURI,'icon',dtype=TYPE_REFERENCE,data_val=0x7f010000),attr(AURI,'theme',dtype=TYPE_REFERENCE,data_val=0x01030009)]);xml+=sel('activity',[attr(AURI,'name','com.marko.auralis.MainActivity'),attr(AURI,'exported',dtype=TYPE_INT_BOOLEAN,data_val=0xffffffff)]);xml+=sel('intent-filter',[]);xml+=sel('action',[attr(AURI,'name','android.intent.action.MAIN')]);xml+=eel('action');xml+=sel('category',[attr(AURI,'name','android.intent.category.LAUNCHER')]);xml+=eel('category');xml+=eel('intent-filter');xml+=eel('activity');xml+=eel('application');xml+=eel('manifest');xml+=ens(APFX,AURI)
manifest=struct.pack('<HHI',RES_XML_TYPE,8,8+len(xml))+xml;(OUT/'AndroidManifest.xml').write_bytes(manifest)

html=(ROOT/'assets/index.html').read_bytes();icon=(ROOT/'assets/app_icon.png').read_bytes();(OUT/'index.html').write_bytes(html);(OUT/'app_icon.png').write_bytes(icon)
unsigned=OUT/'Auralis_v2.0.1_unsigned.apk'
with zipfile.ZipFile(unsigned,'w',compression=zipfile.ZIP_DEFLATED) as z:
    z.write(OUT/'AndroidManifest.xml','AndroidManifest.xml',compress_type=zipfile.ZIP_STORED)
    z.write(OUT/'resources.arsc','resources.arsc',compress_type=zipfile.ZIP_STORED)
    z.write(OUT/'classes.dex','classes.dex',compress_type=zipfile.ZIP_DEFLATED)
    z.write(OUT/'index.html','assets/index.html',compress_type=zipfile.ZIP_DEFLATED)
    z.write(OUT/'app_icon.png','assets/app_icon.png',compress_type=zipfile.ZIP_DEFLATED)
    z.write(OUT/'app_icon.png','res/drawable/app_icon.png',compress_type=zipfile.ZIP_DEFLATED)
print('unsigned',unsigned,'size',unsigned.stat().st_size,'dex',len(buf),'methods',method_ids_size,'code',code_count)
