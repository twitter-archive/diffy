/****
 * Grapnel.js
 * https://github.com/EngineeringMode/Grapnel.js
 *
 * @author Greg Sabia Tucker
 * @link http://artificer.io
 * @version 0.4.2
 *
 * Released under MIT License. See LICENSE.txt or http://opensource.org/licenses/MIT
*/

(function(t){function e(){"use strict"
var t=this
return this.events={},this.params=[],this.state=null,this.version="0.4.2",this.anchor={defaultHash:window.location.hash,get:function(){return window.location.hash?window.location.hash.split("#")[1]:""},set:function(e){return window.location.hash=e?e:"",t},clear:function(){return this.set(!1)},reset:function(){return this.set(this.defaultHash)}},this._forEach=function(t,e){return"function"==typeof Array.prototype.forEach?Array.prototype.forEach.call(t,e):function(t,e){for(var n=0,r=this.length;r>n;++n)t.call(e,this[n],n,this)}.call(t,e)},this.trigger=function(e){var n=Array.prototype.slice.call(arguments,1)
return this.events[e]&&this._forEach(this.events[e],function(e){e.apply(t,n)}),this},"function"==typeof window.onhashchange&&this.on("hashchange",window.onhashchange),window.onhashchange=function(){t.trigger("hashchange")},this}e.regexRoute=function(t,e,n,r){return t instanceof RegExp?t:(t instanceof Array&&(t="("+t.join("|")+")"),t=t.concat(r?"":"/?").replace(/\/\(/g,"(?:/").replace(/\+/g,"__plus__").replace(/(\/)?(\.)?:(\w+)(?:(\(.*?\)))?(\?)?/g,function(t,n,r,a,o,i){return e.push({name:a,optional:!!i}),n=n||"",""+(i?"":n)+"(?:"+(i?n:"")+(r||"")+(o||r&&"([^/.]+?)"||"([^/]+?)")+")"+(i||"")}).replace(/([\/.])/g,"\\$1").replace(/__plus__/g,"(.+)").replace(/\*/g,"(.*)"),RegExp("^"+t+"$",n?"":"i"))},e.prototype.get=e.prototype.add=function(t,n){var r=this,a=[],o=e.regexRoute(t,a),i=function(){var e=r.anchor.get().match(o)
if(e){var i={params:{},keys:a,matches:e.slice(1)}
r._forEach(i.matches,function(t,e){var n=a[e]&&a[e].name?a[e].name:e
i.params[n]=t?decodeURIComponent(t):void 0})
var c={route:t,value:r.anchor.get(),params:i.params,regex:e,propagateEvent:!0,previousState:r.state,preventDefault:function(){this.propagateEvent=!1},callback:function(){n.call(r,i,c)}}
if(r.trigger("match",c),!c.propagateEvent)return r
r.state=c,c.callback()}return r}
return i().on("hashchange",i)},e.prototype.on=e.prototype.bind=function(t,e){var n=this,r=t.split(" ")
return this._forEach(r,function(t){n.events[t]?n.events[t].push(e):n.events[t]=[e]}),this},e.Router=e.prototype.router=e,e.prototype.context=function(t){var e=this
return function(n,r){var a="/"!==t.slice(-1)?t+"/":t,o=a+n
return e.get.call(e,o,r)}},e.listen=function(t){return function(){for(var e in t)this.get.call(this,e,t[e])
return this}.call(new e)},"function"==typeof t.define?t.define(function(){return e}):"object"==typeof exports?exports.Grapnel=e:t.Grapnel=e}).call({},window)