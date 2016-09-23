////////////////////////////////////////////////////////////////////////////////////////////////////
//
// WeightsGenerator
//
// * Facilitates generation of weight values for wchoose or Pwrand
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

WeightsGenerator {
	var <>sliders, dataDisplays, labels;
	var <data, <label, <configurations, <weights, <win;
	var initProb, probs, numProbs, interfaceOn;

	*new { |data, label = " "|
		^super.new.init(data, label);
	}

	init { |data_, label_|
		data = data_;
		label = label_;
		initProb = 0.01;
		numProbs = data.size;
		probs = Array.fill(numProbs, initProb);
		weights = probs.normalizeSum;
		configurations = ();
		sliders = ();
		dataDisplays = ();
		labels = ();
		interfaceOn = false;
		if(data.isNil) { "You didn't feed me data!".postln };
	}

	set {|vals|
		if(vals.size != numProbs) {
			"Values do not match data size, provide % values".format(numProbs).postln
		} {
			if(interfaceOn) {
				numProbs.do{|i| sliders[i].valueAction = vals[i] };
			} {
				probs = vals;
				weights = probs.normalizeSum;
			}
		}
	}

	/*
	Obviously setting all to same value
	results in same weight for all,
	no matter the value, however, this value
	must be different than 0, else results in nil
	*/
	setAll {|val|
		if(interfaceOn) {
			numProbs.do{|i| sliders[i].valueAction = val };
		} {
			numProbs.do{|i| probs.put(i, val) };
			weights = probs.normalizeSum;
		}
	}

	makeInterface { | xPos = 0, yPos = 600, masterWin |
		var styler = GUIStyler(masterWin);
		var winWidth = styler.ezSizeW + (styler.gadgetWidth/2);
		var winHeight = (numProbs * styler.gadgetHeight) + (styler.gadgetHeight * 6.5);
		var randBtn, storeBtn, printBtn, uniformBtn;
		var decorator, offsetX;
		var current = 0;
		interfaceOn = true;
		win = styler.getWindow("",Rect(xPos, yPos, winWidth, winHeight));
		win.decorator = FlowLayout(win.bounds);
		decorator = win.decorator;

		styler.getSubtitleText(win, "% weights".format(label), decorator);

		numProbs.do{|i|
			sliders.put(i,
				styler.getEZSlider(win, "%".format(data[i]), ControlSpec(0.01, 1.0, step: 0.01), \horz)
				.action_({ |sl|
					weights = probs.put(i, sl.value).normalizeSum;
				});
			);
		};

		decorator.nextLine;

		randBtn = styler.getButton(win, "rand");
		storeBtn = styler.getButton(win, "store");
		printBtn = styler.getButton(win, "print");
		uniformBtn = styler.getButton(win, "uniform");

		randBtn
		.action = { sliders.do{|sl| sl.valueAction = rrand(0.0, 1.0) }};

		storeBtn
		.action = {
			var slidersData = (sliders.size).collect{|i| sliders[i].value };
			configurations.put(current, slidersData);
			"Configuration % stored".format(current).postln;
			current = current + 1;
		};

		printBtn
		.action = {
			var size = configurations.size;
			if(size > 0) {
				size.do{|i|
					"Configuration %: %".format(i, configurations[i]).postln
				}
			}  { "Nothing stored".postln }
		};

		uniformBtn
		.action = {
			this.setAll(0.01);
		};

		this.set(probs); // Set probs to current prob
		//win.front;
	}

}