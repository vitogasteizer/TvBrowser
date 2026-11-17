package com.example.tvbrowser

object RemotePage {
    const val HTML = """<!doctype html>
<html lang="ka"><head><meta charset="utf-8">
<meta name="viewport" content="width=device-width,initial-scale=1,maximum-scale=1,user-scalable=no">
<title>TV პულტი</title>
<style>
*{box-sizing:border-box;-webkit-tap-highlight-color:transparent}
html,body{margin:0;height:100%;overflow:hidden;overscroll-behavior:none;background:#0E1116;color:#F2F4F8;font-family:system-ui,'Noto Sans Georgian',sans-serif;touch-action:none}
body{display:flex;flex-direction:column;gap:14px;padding:18px}
header{display:flex;align-items:center;justify-content:space-between;height:44px}
.st{display:flex;align-items:center;gap:10px;font-weight:600}
#dot{width:10px;height:10px;border-radius:5px;background:#FF8A7A}#dot.ok{background:#6EE7A8}
.seg{display:flex;background:#171B22;border-radius:22px;padding:4px;gap:2px}
.seg button{height:36px;padding:0 14px;border-radius:18px;border:0;background:transparent;color:#9AA4B2;font-weight:600;font-size:14px;font-family:inherit}
.seg .on{background:#6EA8FF;color:#0B1220}
#pad{flex:1;position:relative;border-radius:28px;background:#171B22;border:1px solid #262C36;display:flex;align-items:center;justify-content:center;text-align:center;color:#9AA4B2;line-height:1.6;padding:0 70px;font-size:15px}
#scr{position:absolute;right:12px;top:16px;bottom:16px;width:44px;border-radius:22px;background:#222833;display:flex;flex-direction:column;align-items:center;justify-content:space-between;padding:12px 0;color:#F2F4F8;font-size:12px}
.row{display:flex;gap:10px}
.row button{flex:1;height:68px;border-radius:22px;border:0;background:#222833;color:#F2F4F8;font-weight:600;font-size:13px;font-family:inherit}
.row .acc{background:#6EA8FF;color:#0B1220;font-size:16px;font-weight:700}
.tx{display:flex;gap:10px}
.tx input{flex:1;min-width:0;height:56px;border-radius:28px;border:1px solid #262C36;background:#171B22;color:#F2F4F8;padding:0 20px;font-size:16px;touch-action:auto;font-family:inherit}
.tx button{height:56px;padding:0 22px;border-radius:28px;border:0;background:#6EA8FF;color:#0B1220;font-weight:700;font-size:16px;font-family:inherit}
</style></head><body>
<header><div class="st"><span id="dot"></span><span id="stx">კავშირი…</span></div>
<div class="seg"><button id="m0" class="on">მაუსი</button><button id="m1">ღილაკები</button></div></header>
<div id="pad"><div id="hint">გადაათრიე კურსორის გასაადგილებლად<br>შეხება = კლიკი</div>
<div id="scr"><span>▲</span><span>გადახვევა</span><span>▼</span></div></div>
<div class="row"><button data-k="back">უკან</button><button data-k="reload">განახლება</button><button data-k="full">სრული ეკრანი</button><button data-k="ok" class="acc">OK</button></div>
<div class="tx"><input id="t" placeholder="ტექსტის აკრეფა…" autocomplete="off" autocapitalize="off"><button id="send">გაგზავნა</button></div>
<script>
var g=function(i){return document.getElementById(i)};
var K=new URLSearchParams(location.search).get('k'),ws=null,mode=0;
function conn(){
 ws=new WebSocket('ws://'+location.host+'/ws?k='+K);
 ws.onopen=function(){g('dot').className='ok';g('stx').textContent='დაკავშირებულია';send('mode '+mode);};
 ws.onclose=function(){g('dot').className='';g('stx').textContent='კავშირი…';setTimeout(conn,1000);};
}
function send(s){if(ws&&ws.readyState==1)ws.send(s);}
function setMode(m){mode=m;g('m0').className=m==0?'on':'';g('m1').className=m==1?'on':'';
 g('hint').innerHTML=m==0?'გადაათრიე კურსორის გასაადგილებლად<br>შეხება = კლიკი':'გადაასრიალე მიმართულებისთვის<br>შეხება = OK';send('mode '+m);}
g('m0').onclick=function(){setMode(0)};g('m1').onclick=function(){setMode(1)};
var pad=g('pad'),lx=0,ly=0,sx=0,sy=0,t0=0,moved=0,ax=0,ay=0,tm=null;
function flush(){if(tm)return;tm=setTimeout(function(){tm=null;if(mode==0&&(ax||ay))send('m '+Math.round(ax)+' '+Math.round(ay));ax=0;ay=0;},25);}
pad.addEventListener('touchstart',function(e){if(e.target.closest('#scr'))return;var t=e.touches[0];lx=sx=t.clientX;ly=sy=t.clientY;t0=Date.now();moved=0;e.preventDefault();},{passive:false});
pad.addEventListener('touchmove',function(e){if(e.target.closest('#scr'))return;var t=e.touches[0];ax+=t.clientX-lx;ay+=t.clientY-ly;lx=t.clientX;ly=t.clientY;moved=Math.max(moved,Math.abs(t.clientX-sx)+Math.abs(t.clientY-sy));e.preventDefault();flush();},{passive:false});
pad.addEventListener('touchend',function(e){if(e.target.closest('#scr'))return;
 if(moved<10&&Date.now()-t0<350){send('c');return;}
 if(mode==1&&moved>=40){var dx=lx-sx,dy=ly-sy;if(Math.abs(dx)>Math.abs(dy))send('d '+(dx>0?'r':'l'));else send('d '+(dy>0?'dn':'up'));}
});
var sc=g('scr'),sy0=0;
sc.addEventListener('touchstart',function(e){sy0=e.touches[0].clientY;e.preventDefault();},{passive:false});
sc.addEventListener('touchmove',function(e){var y=e.touches[0].clientY;send('s '+Math.round(sy0-y));sy0=y;e.preventDefault();},{passive:false});
document.querySelectorAll('[data-k]').forEach(function(b){b.onclick=function(){send('k '+b.getAttribute('data-k'));};});
var ti=g('t');
function sendText(){if(ti.value){send('t '+ti.value);ti.value='';}}
g('send').onclick=sendText;
ti.addEventListener('keydown',function(e){
 if(e.key=='Enter'){sendText();send('e');e.preventDefault();}
 else if(e.key=='Backspace'&&!ti.value){send('b');}
});
conn();
</script></body></html>"""
}
