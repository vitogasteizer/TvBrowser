package com.example.tvbrowser

object Js {
    const val INIT = """
(function(){
if(window.__tvb)return;window.__tvb=1;
function vis(r){return r.width>240&&r.height>135&&r.bottom>0&&r.right>0&&r.top<innerHeight&&r.left<innerWidth;}
function best(){
 var a=0,b=null,i,r,vs=document.getElementsByTagName('video');
 for(i=0;i<vs.length;i++){r=vs[i].getBoundingClientRect();if(vis(r)&&r.width*r.height>a){a=r.width*r.height;b=vs[i];}}
 if(b)return b;
 var fs=document.getElementsByTagName('iframe');
 for(i=0;i<fs.length;i++){r=fs[i].getBoundingClientRect();
  if(vis(r)&&r.width>400&&/youtube|vimeo|player|embed|stream|video|dailymotion|twitch/i.test(fs[i].src||'')&&r.width*r.height>a){a=r.width*r.height;b=fs[i];}}
 return b;}
function fsEl(){return document.fullscreenElement||document.webkitFullscreenElement;}
var last=false;
setInterval(function(){var f=!!best()&&!fsEl()&&!window.__tvbP;if(f!==last){last=f;try{TVB.video(f);}catch(e){}}},1200);
function pseudo(v){window.__tvbP={el:v,css:v.style.cssText,ctl:v.controls};
 v.style.cssText='position:fixed;top:0;left:0;width:100vw;height:100vh;z-index:2147483647;background:#000;object-fit:contain';
 if(v.tagName=='VIDEO')v.controls=true;try{TVB.pseudo(true);}catch(e){}}
window.__tvbExit=function(){var p=window.__tvbP;if(!p)return;p.el.style.cssText=p.css;if(p.el.tagName=='VIDEO')p.el.controls=p.ctl;window.__tvbP=null;try{TVB.pseudo(false);}catch(e){}};
window.__tvbFull=function(){var v=best();if(!v)return;var p;
 try{p=(v.requestFullscreen||v.webkitRequestFullscreen).call(v);}catch(e){p=null;}
 if(p&&p.catch){p.catch(function(){pseudo(v);});}
 else{setTimeout(function(){if(!fsEl())pseudo(v);},500);}};
})();
"""

    const val FIELD = """
(function(){
if(window.__tvbF)return;window.__tvbF=1;
function isField(e){if(!e)return false;var t=e.tagName;
 if(t=='INPUT'){var y=(e.type||'text').toLowerCase();return ['checkbox','radio','button','submit','range','file','color','image','reset'].indexOf(y)<0;}
 return t=='TEXTAREA'||!!e.isContentEditable;}
document.addEventListener('focusin',function(e){if(isField(e.target)){try{TVB.field(true);}catch(x){}}},true);
document.addEventListener('focusout',function(){setTimeout(function(){if(!isField(document.activeElement)){try{TVB.field(false);}catch(x){}}},60);},true);
})();
"""

    const val TOGGLE = """
(function(){var v=document.fullscreenElement||document.webkitFullscreenElement;
if(!v||v.tagName!='VIDEO')v=(window.__tvbP&&window.__tvbP.el)||document.querySelector('video');
if(v&&v.tagName=='VIDEO'){if(v.paused)v.play();else v.pause();}})();
"""

    const val SEEK = """
(function(d){var v=document.fullscreenElement||document.webkitFullscreenElement;
if(!v||v.tagName!='VIDEO')v=(window.__tvbP&&window.__tvbP.el)||document.querySelector('video');
if(v&&v.tagName=='VIDEO'){v.currentTime=Math.max(0,v.currentTime+d);}})(__D__);
"""

    const val TYPE = """
(function(t){var e=document.activeElement;if(!e)return;
try{
 if(e.tagName=='INPUT'||e.tagName=='TEXTAREA'){
  var pr=e.tagName=='INPUT'?HTMLInputElement.prototype:HTMLTextAreaElement.prototype;
  var st=Object.getOwnPropertyDescriptor(pr,'value').set;
  var s=e.selectionStart,n=e.selectionEnd,v=e.value;
  if(typeof s!='number'){s=n=v.length;}
  st.call(e,v.slice(0,s)+t+v.slice(n));
  try{e.setSelectionRange(s+t.length,s+t.length);}catch(x){}
  e.dispatchEvent(new Event('input',{bubbles:true}));
  e.dispatchEvent(new Event('change',{bubbles:true}));
 }else if(e.isContentEditable){document.execCommand('insertText',false,t);}
}catch(x){}})(__T__);
"""
}
