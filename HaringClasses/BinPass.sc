////////////////////////////////////////////////////////////////////////////////////////////////////
//
//  BinPass, a bin magnitude filter (FFT processing)
//
// * Scales spectral magnitudes according to a multiplying buffer
// * Facilities for sprectral or post IFFT effects
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
// along with this program.  If not, see <http://www.gnu.org/licenses/>.

BinPass {
	// Private
	var sampleRate, presetCounter, dataTags, params, initVals, hertzRatio, reverbState;
	var sndSource, zeroVals, morphTypes, interpolateCondition, current, customFileMenu;
	// Getters
	var <frames, <numBands, <binRange, <binWidth, <totalBins, <usefulBins, <currentEq;
	var <dataSet, <customDataSet, <eqBuffer, <instrument, <reverb, <interpolationData;
	var <morpher, <bands, <skipjack, <customTags, <customEQfiles, <effects, <reverbParams;
	// Setters/Getters
	var <>masterPath, <>savedPresets, <>dataPaths, <>bufferPaths;
	var <>morphTime, <>stepsPerSec, <>curve, <>currentMorphType;
	var <>multi;

	*new { |frames = 512|
		^super.new.init(frames);
	}

	init { |frames_|
		var server = Server.local;
		if(server.serverRunning) {
			masterPath = "/Users/darienbrito/Documents/Work/AtomataRemixer/";
			dataPaths = (masterPath  ++ "EQPresets/*").pathMatch;
			sampleRate = Server.local.sampleRate;
			frames =  frames_;
			numBands = (frames / 4).asInteger;
			binRange = (frames / numBands) / 2;
			hertzRatio = sampleRate / frames;
			binWidth = (sampleRate / 2) / frames;
			totalBins = frames / 2;
			eqBuffer = Buffer.alloc(Server.local, frames, 1).zero;
			presetCounter = Pseries(0,1,inf).asStream;
			dataTags = (dataPaths.size).collect{|i| (\file++i).asSymbol};
			params = [\rate, \amp, \stretch,\shift];
			reverbParams = [\roomsize, \revtime, \damping, \inputbw, \spread, \drylevel, \earlylevel, \taillevel];
			initVals = [1.0, 1.0, 1.0, 0.0];
			reverbState = false;
			sndSource = Group(Server.local);
			effects = Group.after(sndSource);
			zeroVals = Array.fill(numBands, 0);
			current = zeroVals;
			currentEq = zeroVals;
			savedPresets = ();
			dataSet = ();
			customDataSet = ();
			morphTime = 2;
			stepsPerSec = 20;
			curve = 4;
			morphTypes =
			[
				\lin, \exp, \explin,\lincurve,\curvelin, \ramp,
				\bilin,\biexp,\lcurve,\scurve,\gausscurve,\custom
			];
			currentMorphType = morphTypes[0];
			morpher = Morpher(morphTime, stepsPerSec, curve);
			interpolateCondition = false;
			bands = this.getBands;
			this.makeInterpolationData;
		} { this.serverOffWarning;}
	}

	getBands {
		var binSet, bands, binSections, tags;
		var limit = numBands - 1;
		bands = List();
		binSet = Array.fill(numBands + 1, {|i| i * binRange});
		numBands.do{|i| bands.add([binSet[i], binSet[i+1]])}
		^bands;
	}

	runSynth { |inBuf = 1, pos = 0, rate = 1, amp = 1, stretch = 1, shift = 0, loop = 0 |
		if(inBuf != nil) {
			if(inBuf != eqBuffer.bufnum) {
				instrument = Synth(\Haring_binPassFilter,
					[
						\frames, frames, \inBuffer, inBuf, \binFilter, eqBuffer, \pos, pos,
						\rate, rate, \amp, amp, \stretch, stretch, \shift, shift, \loop, loop
					],
					target: sndSource);
				reverb = Synth(\Haring_reverb,[\wet, 0.0], target: effects);
				effects.run(reverbState) // Default is no effects on load
			} { this.reservedBufferError}
		} { this.noBufferError }
	}

	play { |inBuf = 1, pos = 0, rate = 1, amp = 1, stretch = 1, shift = 0, loop = 0 |
		this.runSynth(inBuf, pos, rate, amp, stretch, shift, loop);
		effects.run(reverbState);
		"playing".postln;
	}

	pause {
		sndSource.run(false);
		effects.run(false);
		"paused".postln
	}

	resume {
		sndSource.run(true);
		effects.run(reverbState);
		"resumed".postln;
	}

	stop {
		instrument.free;
		reverb.free;
		"off".postln
	}

	setPlayer { | params |
		if(params.size.odd) { this.playerError } {
			params.pairsDo({|key, value|
				instrument.set(key, value);
			})
		}
	}

	setReverb { | reverbActive = false, params |
		reverbState = reverbActive;
		effects.run(reverbState);
		if(params != nil) {
			if(params.size.even) {
				params.pairsDo({|key, value|
					reverb.set(key, value)
				});
			} { this.reverbError }
		}
	}

	getNegativeEQ {
		^(1 - currentEq)
	}

	storeEQ {
		var count = presetCounter.next;
		if( count < 1 ) { "-> Will store in \"savedPresets\" object".postln };
		"Stored: %".format(count).postln;
		savedPresets.put(count, currentEq);
	}

	setEQVals { |vals|
		if(vals.size != numBands) {
			"You must provide an array of % elements".format(numBands).postln
		} {
			vals = vals.asFloat; // Make sure they are floats
			currentEq = vals; // Different than "current"
			vals.do{|v, i| eqBuffer.set( i * 2, v)}
		}
	}

	getFilterData {|paths|
		var folderPath, find, tags;
		customEQfiles = paths;
		customTags = customEQfiles.collect{|val, i| (\eq++i).asSymbol };
		"Custom data acquired. % files selected.".format(customEQfiles.size).postln;
		this.loadFilterData(customEQfiles);
	}

	getFilterDataDialog {
		Dialog.openPanel({|paths|
			this.getFilterData(paths);
			if(customFileMenu != nil) { customFileMenu.items = customTags };
		}, multipleSelection: true);
	}

	loadFilterData { |inputData|
		inputData.do{|filePath, i|
			var file = File(filePath, "r");
			var data = List();
			numBands.do{|j| data.add(file.getLine().asFloat)};
			customDataSet.put(customTags[i], data)
		};
	}

	makeInterpolationData {
		interpolationData = morpher.getVarInterpolationData(morphTime);
	}

	makeDrawPlotter {|time, type|
		time = time ? morphTime;
		type = type ? currentMorphType;
		morphTime = time;
		currentMorphType = type;
		morpher.getPlotter(currentMorphType, morphTime);
	}

	morphTo { |start, end, time, type = \lin|
		var retrieveSpeed = 1/morpher.getStepsPerSec;
		var check;
		start = start ? currentEq;
		time = time ? morphTime;
		if(end!=nil) {
			check = TaskProxy({
				morpher.morphTo(start, end, time, type);
				while({morpher.isActive}, {
					current = morpher.getCurrent;
					this.setEQVals(current);
					retrieveSpeed.wait;
				});
				current = morpher.getCurrent;
				this.setEQVals(current);
				check.stop
			}).quant_(0).play;
		} { "Provide at least a target".postln }
	}

	exportEqPresets {
		var pathFolder = masterPath++"CustomEQPresets";
		var date = Date.getDate.format("%T");
		var time = Date.getDate.format("%F");
		var timeStamp = (date + time).replace(" ", "_");
		var exportFolder = pathFolder +/+ timeStamp;
		if(savedPresets.size < 1) { "There is nothing to export".postln } {
			exportFolder.mkdir;
			(savedPresets).do{|val, tag|
				var prepend, pathFile, file, count;
				case
				{ tag < 10 } { count = (\00++tag).asString }
				{ tag < 100 } { count = (\0++tag).asString }
				{ tag < 1000 } { count = (tag).asString };
				pathFile = exportFolder +/+ "eqSetting_" ++ count ++ ".txt";
				file = File(pathFile, "w");
				val.do{|v| file.write(v.asString++"\n")};
				file.close;
			};
			"Presets exported to: %".format(pathFolder).postln;
		}
	}

	reportData { |obj|
		var index = obj.index;
		var currentValue = obj.currentValue;
		"Band: %, frequency range: %, amplitude: %"
		.format(index, bands[index] * hertzRatio, currentValue)
		.postln;
	}

	makeInterface { |inBuf = 1, pos = 0, rate = 1, amp = 1, stretch = 1, shift = 0, winPosX = 0, winPosY = 600, label = "" |
		var win = Window("BinEQ" ++ label, Rect(winPosX, winPosY, (numBands * 4)+14, 210));
		var clearBtn , rndBtn, presetBtn, morphBtn, timeBox, iSelectMode, drawBtn;
		var setsMenu , revBtn, exportBtn, customEqbtn, playBtn, pauseBtn;
		var bwidth = 80, bheight = 20;
		var btnSize = bwidth-12@bheight;
		var fill = Color.rand;

		if(inBuf != nil) {
			if(inBuf != eqBuffer.bufnum) {

				win.view.decorator = FlowLayout(win.view.bounds);
				multi = MultiSliderView(win, Rect(0, 0, (numBands * 4)+5, 100 ));
				multi.value = zeroVals;
				multi.isFilled_(true);
				multi.indexThumbSize_(3);
				multi.gap_(1);
				multi.drawLines_(true);
				multi.fillColor_(fill);
				multi.strokeColor_(fill);
				multi.action = { |a|
					var dataArray = a.value;
					this.setEQVals(dataArray);
					current = dataArray;
					//this.reportData(index, a);
				};

				// amp is given with sc
				Spec.add(\rate,[-2.0, 2.0]);
				Spec.add(\stretch,[0.01, 3.0]);
				Spec.add(\shift,[-5.0, 5.0 ]);
				Spec.add(\wet,[0.0, 1.0]);

				clearBtn = Button(win, bwidth*0.75@bheight);
				rndBtn = Button(win, bwidth*0.75@bheight);
				presetBtn = Button(win, bwidth*0.5@bheight);
				morphBtn = Button(win, (bwidth*0.25)@bheight);
				drawBtn = Button(win, (bwidth*0.5)@bheight);
				timeBox = NumberBox(win, (bwidth * 0.5)@bheight);
				iSelectMode = PopUpMenu(win, (bwidth*0.5)@bheight);
				setsMenu = PopUpMenu(win, bwidth*0.5@bheight);
				exportBtn = Button(win, btnSize);
				revBtn = Button(win, btnSize);

				params.do{|spec, i|
					EZSlider(win, bwidth*3@bheight, spec, spec, {|ez|
						instrument.set(spec, ez.value)
					}, initVal: initVals[i])
				};

				EZSlider(win, bwidth*3@bheight, \wet, \wet, {|ez|
					reverb.set(\wet, ez.value)
				}, initVal: 0.0);

				playBtn = Button(win, btnSize);
				pauseBtn = Button(win, btnSize);
				customEqbtn = Button(win, btnSize);
				customFileMenu = PopUpMenu(win, (bwidth*0.6)@bheight); // is global

				clearBtn
				.states_([ ["clear", Color.black, Color.white] ])
				.action_({
					var target = zeroVals;
					if(interpolateCondition) {
						this.morphTo(current, target, morphTime, currentMorphType);
					} {
						multi.valueAction_(target);
					}
				});
				rndBtn
				.states_([ ["random", Color.black, Color.white] ])
				.action_({
					var iter = numBands.asInt;
					var target = Pbrown(0,1,0.125).asStream.nextN(numBands);
					if(interpolateCondition) {
						this.morphTo(current, target, morphTime, currentMorphType);
					} {
						multi.valueAction_(target);
					}
				});
				presetBtn
				.states_([["store", Color.black, Color.white] ])
				.action_({
					var count = presetCounter.next;
					if( count < 1 ) { "-> Will save to \"f.savedPresets\" object".postln };
					"Stored: %".format(count).postln;
					savedPresets.put(count, multi.value);
					setsMenu.items = (count + 1).collect{|i| i }; //Grow sets when pressed
					setsMenu.value = count;
				});
				morphBtn
				.states_([ ["i",Color.black, Color.white], ["x", Color.black, Color.red] ])
				.action_({|btn|
					if(btn.value > 0) {
						interpolateCondition = true;
						"Interpolation on".postln;
						skipjack.play;
					} {
						interpolateCondition = false;
						"Interpolation off".postln;
						skipjack.stop;
					}
				});
				timeBox
				.value_(morphTime)
				.action_({|box|
					morphTime = box.value;
				})
				.align_("center");

				iSelectMode
				.items_(morphTypes)
				.action_({|pop|
					var mode = morphTypes[pop.value];
					currentMorphType = mode;
					if(mode == \custom) { "Custom interpolation will not be kept after playing...".postln }
				});
				setsMenu
				.items_([" "])
				.action_({|pop|
					var set = pop.value;
					var target = savedPresets[set];
					if(interpolateCondition) {
						this.morphTo(current, target, morphTime, currentMorphType);
					} {
						multi.valueAction_(target);
					}
				});
				revBtn
				.states_([ ["reverb", Color.black, Color.white], ["off", Color.black, Color.red] ])
				.action_({ |btn|
					if(btn.value > 0) {reverbState = true} {reverbState = false};
					effects.run(reverbState);
				});
				exportBtn
				.states_([ ["export",Color.black, Color.white] ])
				.action_({|btn|
					this.exportEqPresets
				});
				drawBtn
				.states_([ ["plot", Color.black, Color.white]])
				.action_({|btn|
					this.makeDrawPlotter();
				});
				customEqbtn
				.states_([ ["load",Color.black, Color.white]] )
				.action_({|btn|
					this.getFilterDataDialog;
				});
				customFileMenu.action_({|pop|
					var index = pop.value;
					var target = customDataSet[customTags[index]].asFloat;
					if(interpolateCondition) {
						this.morphTo(current, target, morphTime, currentMorphType);
					} {
						multi.valueAction_(target);
					}
				});
				playBtn
				.states_([ ["play", Color.black, Color.white],["stop", Color.black, Color.white] ])
				.action_({|btn|
					if(btn.value > 0) {
						this.play;
					} {
						this.stop;
					}
				});
				pauseBtn
				.states_([ ["pause", Color.black, Color.white],["resume", Color.black, Color.white] ])
				.action_({|btn|
					if(btn.value > 0) {
						this.pause;
					} {
						this.resume;
					}
				});

				skipjack = SkipJack({
					multi.value = current; //Update multisliders view
				}, 0.05, { win.isClosed }, name: "multiSlidersUpdate", autostart: false);

				win.onClose = {
					//instrument.free;
					//eqBuffer.zero;
					skipjack.stop;
					"-> I hope you enjoyed messing around those bins...".postln
				};

				win.front;

			} { this.reservedBufferError }
		} { this.noBufferError }
	}

	info {
		"\n-> ----------[ BinPassFilter prototype]-------".postln;
		"-> %hz sample-rate, % frames.".format(sampleRate, frames).postln;
		"-> Hertz to bin ratio: %".format(hertzRatio).postln;
		"-> Bin-width: % hertz".format(binWidth).postln;
		"-> Total of % bins".format(totalBins).postln;
		"-> Total of % useful bins".format(numBands).postln;
		"-------------------------------------------";
	}

	reservedBufferError {
		"Buffer reserved for bin multiplication".error;
		"Please try another buffer number...".postln;
	}

	noBufferError {
		"No soundfile buffer number provided".error;
		"Please enter a buffer number...".postln;
	}

	serverOffWarning {
		"Server must be ON to initialize BinPass".warn;
		"Please turn on the server first...".postln;
	}

	playerError {
		var synthParams = [\inBuffer] ++ [\rate, \amp, \stretch,\shift];
		"Wrong input. You need to provide pairs of tags and vals".error;
		"possible parameters are: %".format(params).postln;
	}

	reverbError{
		var reverbParams = [\wet] ++ reverbParams;
		"Wrong input. You need to provide pairs of tags and vals".error;
		"possible parameters are: %".format(reverbParams).postln;
	}


}