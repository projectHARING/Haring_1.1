////////////////////////////////////////////////////////////////////////////////////////////////////
//
// ProbabilityGenerator
//
// * Facilitates generation of values based on tendency masks and probability
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

ProbabilityGenerator {
	//Getters
	var <obj, <params, <configurations, <tags, <rangers;
	var <dataDisplays, <upperDataDisplays, <labels, <ranges;
	var <interfaceOn, <values, <name, <rangersNum, <win, <outPairs;
	var <wetSl, <probSl;
	//Setters
	var <>wet;
	var <>probability;
	var <>exportPath;
	//Private
	var num;

	*new { | obj, params, customName |
		^super.new.init(obj, params, customName);
	}

	// Wet and prob are handled separately...
	init { |obj_, params_, customName_|

		if(params.size.odd) { "C'mon! I'm expecting paired parameters!".warn } {

			obj = obj_;
			if(customName_.isNil) { name = obj.key } { name = customName_ };
			params = params_;
			wet = 0.0;
			probability = 0.5;
			configurations = ();
			tags = ();
			rangers = ();
			upperDataDisplays = ();
			dataDisplays = ();
			labels = List();
			ranges = List();
			interfaceOn = false;
			params.pairsDo{ |label, range|
				labels.add(label);
				ranges.add(range);
			};
			num = labels.size;
			values = Array.fill(num, {|i| [ranges[i][0],ranges[i][1]] }); // By default min % max vals
			exportPath = "/Users/darienbrito/Documents/Work/AutomataRemixer/";
		}
	}

	/*
	Be aware that when entering data from code the
	ranges are withing the boundaries of the ranges
	entered at instancing. I could write a method to
	check automatically but I'm lazy at the moment.

	Also, paired params are not actually necessary
	but made it like this so is easier for the user
	to know what input data is what by having tags.
	See "internalSet" method.
	*/
	set {| paramPairs |
		if((paramPairs.size/2) != (num)) {
			"Pairs do not match data size, provide % pairs".format(num).postln
		} {
			var inputVals = List();
			paramPairs.pairsDo{ |tag, pair| inputVals.add(pair) };
			if(interfaceOn) {
				num.do{|i|
					rangers[i].valueAction_(inputVals[i])
				};
			} {
				inputVals.do{|pair, i|
					values.put(i, pair);
				}
			}
		}
	}

	loadSet { |params|
		if(interfaceOn) {
			params.do{|pair, i|
				rangers[i].valueAction = pair
			};
		} {
			params.do{|pair, i|
				values.put(i, pair);
				// Here a setter
			}
		};
		//"Current profile %".format(values).postln; // Debug
	}

	setAll {|lowVal, hiVal|
		if(interfaceOn) {
			rangersNum.do{|i| rangers[i].valueAction = [lowVal, hiVal] };
		} {
			rangersNum.do{|i| values.put(i, [lowVal, hiVal]) };
		}
	}

	internalSet {
		num.do{|i| rangers[i].valueAction = values[i]};
	}

	setDryWet { | wetVal = 0.0 |
		wet = wetVal;
		if(interfaceOn) {
			rangers[1].valueAction_([0, wet]); // rangers[1] is reverb
		} {
			obj.set(\wet, rrand(0, wet));
		};
		//"Wet: %".format(wet).postln; //Debug
	}

	setProbability { | prob = 50 |
		if(interfaceOn) {
			probSl.valueAction_(prob);
		} {
			probability = prob;
		};
		//"Probability: %".format(prob).postln; //Debug
	}

	exportPresets { | path, configs |
		var profilesPath = "%ParameterProfiles/profiles%.scd".format(path, name);
		var size = configs.size;
		var file = File(profilesPath,"w");
		file.write("[\n");
		size.do{|i|
			var line = configs[i].asString;
			if(i == (size - 1)) {
				file.write(line++"\n"); //Last line
			} {
				file.write(line++",\n");
			}
		};
		file.write("];\n");
		file.close;
		"-> exported to: %".format(profilesPath).postln;
	}

	makeInterface { |posX = 0, posY = 600, masterWin|
		var styler = GUIStyler(masterWin);
		var winWidth = styler.ezSizeW + (styler.gadgetWidth/2);
		var winHeight = styler.gadgetHeight * 6.5;
		var randBtn, storeBtn, printBtn, uniformBtn, exportBtn, clearBtn;
		var decorator, offsetX;
		var current = 0;
		interfaceOn = true;
		win = styler.getWindow(name, Rect(posX, posY, winWidth, winHeight));
		win.decorator = FlowLayout(win.bounds);
		decorator = win.decorator;

		styler.getSubtitleText(win, "Probabilistic Control %".format(name),decorator);
		probSl = styler.getEZSlider(win, "prob", ControlSpec(0, 100,\lin,0.01,50), \horz)
		.action = {|sl|
			probability = sl.value;
		};
		num.do{|i|
			rangers.put(i,
					styler.getEZRanger(win, labels[i], ranges[i].asSpec, \horz)
					.action_({ |sl|
						var hiVal = sl.hi.round(0.01);
						var loVal = sl.lo.round(0.01);
						values.put(i, [loVal, hiVal])
					});
			)
		};

		randBtn = styler.getButton(win, "rand"); // Not very useful
		storeBtn = styler.getButton(win, "store");
		printBtn = styler.getButton(win, "print");
		//uniformBtn = styler.getButton(win, "zero");
		exportBtn = styler.getButton(win, "export");
		clearBtn = styler.getButton(win, "clear");

		randBtn
		.action = {
			num.do{|i|
				rangers[i].lo = rrand(ranges[i][0], ranges[i][1] - ranges[i][0]);
				rangers[i].hi = rrand( ranges[i][1] - ranges[i][0], ranges[i][1]);
				rangers[i].rangeSlider.activeCenter_(rrand(ranges[i][0], ranges[i][1]));
			};
			{num.do{|i| rangers[i].rangeSlider.refresh }}.fork(AppClock); // Otherwise won't refresh
		};

		storeBtn
		.action = {
			var slidersData = num.collect{|i|
				var val = rangers[i].value;
				val.round(0.01);
			};
			configurations.put(current, slidersData);
			"Configuration % stored".format(current+1).postln;
			current = current + 1;
		};
		printBtn
		.action = {
			var size = configurations.size;
			if(size > 0) {
				size.do{|i|
					"Configuration %: %".format(i+1, configurations[i]).postln
				}
			}  { "Nothing stored".postln }
		};

/*		uniformBtn
		.action = {
			this.setAll(0, 0);
			this.setProbability(0);
		};*/

		exportBtn
		.action = {
			this.exportPresets(exportPath, configurations);
		};

		clearBtn
		.action = {
			current = 0;
			configurations = ();
		};

		// Set to current values
		this.setProbability(probability);
		this.setDryWet(wet);
		this.internalSet;
		//win.front;
		win.onClose = { interfaceOn = false };
	}

	setParameters {
		if(probability.coin) {
			"%: lucky change!".format(name).postln;
			num.do{|i|
				var valLo, valHi, out;
				valLo = values[i][0];
				valHi = values[i][1];
				out = rrand(valLo, valHi);
				obj.xset(labels[i], out); // This sets the parameters in the input Ndef based on fadeTime
				"%: %".format(labels[i], out).postln; // Debug
			};
		} {
			"%: no luck...".format(name).postln;
		}
	}
}