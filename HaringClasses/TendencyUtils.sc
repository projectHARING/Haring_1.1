////////////////////////////////////////////////////////////////////////////////////////////////////
//
// Tendency Utilities
//
// * Tendency masks for macro controllers
// * Tendency controllers for tendency masks
//
// Copyright (C) <2016>
//
// by Darien Brito
// http://www.darienbrito.com
//
// This program is free software: you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation, either version 3 of the License, or
// (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program. If not, see <http://www.gnu.org/licenses/>.

TendencyMask {
	var <size, <minBottom, <maxBottom, <minTop, <maxTop, <curv, <time;
	var low_bound, top_bound, times, curves, <>p;
	var <lowBoundProxy, <hiBoundProxy, <timeProxy, <curvesProxy;

	*new { | size,minBottom,maxminBottom,minTop,maxTop,curv=\lin,time=10 |
		^super.newCopyArgs(size, minBottom, maxminBottom, minTop, maxTop, curv, time).init();
	}

	setHi {|min, max|
		var vals = Array.rand(size, min, max);
		hiBoundProxy.source = Pseq(vals, 1);
	}

	setLo { |min, max|
		var vals = Array.rand(size, min, max);
		lowBoundProxy.source = Pseq(vals,1);
	}

	setTime { |time|
		var vals = Array.fill(size,{|i| if(i==size) {0} {time}});
		timeProxy.source = Pseq(vals,1)
	}

	setCurve { |curve|
		var vals = { curve } ! size;
		curvesProxy.source = Pseq(vals,1);
	}

	init {
		lowBoundProxy = PatternProxy();
		hiBoundProxy = PatternProxy();
		timeProxy = PatternProxy();
		curvesProxy = PatternProxy();

		low_bound = Array.rand(size,minBottom,maxBottom);
		top_bound = Array.rand(size,minTop,maxTop);
		times = Array.fill(size,{|i| if(i==size) { 0 }{ time } });
		curves = {curv} ! size;

		lowBoundProxy.source = Pseq(low_bound, 1);
		hiBoundProxy.source = Pseq(top_bound, 1);
		timeProxy.source = Pseq(times, 1);
		curvesProxy.source = Pseq(curves,1);

		p = Pbeta(
			Pseg(
				Pn(lowBoundProxy,inf),
				Pn(timeProxy,inf),
				Pn(curvesProxy,inf)
			),
			Pseg(
				Pn(hiBoundProxy,inf),
				Pn(timeProxy,inf),
				Pn(curvesProxy,inf)
			),
			//Prob1
			0.5,
			0.5
		).asStream;
	}

	variate{
		p = [Pbeta(
			Pseg(
				Pn(lowBoundProxy,inf),
				Pn(timeProxy,inf),
				Pn(curvesProxy,inf)
			),
			Pseg(
				Pn(hiBoundProxy,inf),
				Pn(timeProxy,inf),
				Pn(curvesProxy,inf)
			),
			//Prob1
			0.5,
			0.5
		).asStream,
		Pwhite(
			Pseg(
				Pn(lowBoundProxy,inf),
				Pn(timeProxy,inf),
				Pn(curvesProxy,inf)
			),
			Pseg(
				Pn(hiBoundProxy,inf),
				Pn(timeProxy,inf),
				Pn(curvesProxy,inf)
			)
		).asStream,
		].choose;
	}

	print{|n|
		p.nextN(n).plot;
	}

	next {
		var val = p.next;
		if(val == rrand(minBottom,maxTop)){
			this.variate();
			"variate".postln;
		};
		"Beta val: %".format(val).postln;
		^val;
	}

	next4{
		^p.nextN(4);
	}
}

TendencyControl {
	var <rangers, <title, minRangeSl, maxRangeSl;
	var <currentMin, <currentMax, <interfaceOn;
	var <window, <id, <objs;

	*new { | id, objs |
		^super.new.init(id, objs)
	}

	init { |id_, objs_|
		id = id_;
		objs = objs_;
		rangers = ();
		title = "Time control %".format(id);
		currentMin = [objs[0].minBottom, objs[0].maxBottom]; // All objs should share the same vals
		currentMax = [objs[0].minTop, objs[0].maxTop];
		interfaceOn = false;
		this.setRanges;
	}

	close {
		window.close;
	}

	setRanges {
		if(interfaceOn) {
			rangers[0].valueAction_(currentMin);
			rangers[1].valueAction_(currentMax);
		} {
			//An action for when sliders are not visible
			objs.do{|obj|
				obj.setLo(currentMin[0],currentMin[1]);
				obj.setHi(currentMax[0],currentMax[1]);
			}
		}
	}

	makeInterface { |posX = 0, posY = 0, masterWin|
		var styler = GUIStyler(masterWin);
		var numSliders = 2;
		var winWidth =  styler.ezSizeW + (styler.gadgetWidth/2);
		var winHeight = (styler.gadgetHeight * 4);
		var decorator;
		var tags, label;
		window = styler.getWindow("", Rect(posX, posY, winWidth, winHeight));
		interfaceOn = true;
		window.decorator = FlowLayout(window.bounds);
		decorator = window.decorator;
		label = styler.getSubtitleText(window, title, decorator);
		tags = ["minRanges", "maxRanges"];
		tags.do{|tag, i|
			rangers.put(i,
				styler.getEZRanger(window, tag, ControlSpec(currentMin[0], currentMax[1], \lin, 1), \horz);
			)
		};
		rangers[0].action = {|sl|
			var val = sl.value;
			currentMin = val;
			objs.do{|obj|
				obj.setLo(val[0],val[1]);
			}
		};
		rangers[1].action = {|sl|
			var val = sl.value;
			currentMax = val;
			objs.do{|obj|
				obj.setHi(val[0],val[1]);
			}
		};
		//Set to current
		this.setRanges;
		//window.front;
		masterWin.onClose = { interfaceOn = false }
	}
}