////////////////////////////////////////////////////////////////////////////////////////////////////
//
// Haring
//
// * Wrapper for all the classes and functionality
//
// Copyright (C) <2016>
//
// by Darien Brito & Andrea Vogrig
// [http://www.darienbrito.com , http://andreavogrig.com]
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

Haring {
	classvar frames, p, <b, <d, <m, a, ir, mixer;
	classvar <shapeMorpher, trackSequencers, <timeMacroControllers, allMultiMasks, allSingleMasks;
	classvar masksTags, <masks;
	classvar <revMorpher1, revMorpher2, revMorpher3, revMorpher4;
	classvar morphBtn;
	classvar profilesPath;
	classvar revProfiles, playerProfiles, revProfileData;
	classvar styler;
	classvar playerProbSl, playerProfSl, playerLegend;
	classvar revMacros, playerMacros, paramMorphers, revMorphers;
	classvar <decks;
	classvar infoText;
	classvar win;
	classvar t_seqs, t_seqFades, t_pmorphs, t_rmorphs, t_smorph_fade,  t_smorph_time;
	classvar range;
	classvar load;
	classvar path,decks;
	classvar masterWindow, advancedWindow, creditsWindow, infoWindow;
	classvar alpha;
	classvar divs;

	*new { | path_, decks_ = 4, tracksPath ,divisions,dynamic|
		load = false;
		path = path_;
		decks = decks_;
		alpha = 1;//0.875
		divs = divisions.asInt;
		masterWindow = Window("", Rect(0,800,550,75)).background_(Color.gray(0.2, 1)).alpha_(alpha);
		this.makeGUI;
		b = Allocator(Server.default, path, tracksPath,divisions,dynamic,8,{|res| this.errors_handler(res)});
		infoText.string = "initializing...  ";
	}

	*errors_handler{|res|
		var result;
		result = case
		{res == 0}{	this.start; }
		{res == 1}{ this.message("error while splitting tracks!  "); }
		{res == 2}{ this.message("no audio files were found!  "); };
		result;
	}

	*start{
		this.message("Loading IRs...  ");
		ir = Ir(Server.default,path,{
			this.message("initializing decks...  ");
			this.create();
		});
	}

	*message{|txt|
		{ infoText.string = txt.asString }.fork(AppClock);
	}

	*close{
		if(masterWindow != nil){masterWindow.close;};
		if(advancedWindow != nil){advancedWindow.close;};
		if(infoWindow != nil){infoWindow.close;};
		if(creditsWindow != nil){creditsWindow.close;};

		("sh "++path++"close.sh").unixCmd();
	}

	*create {
		var shapesPath = path ++ "EQPresets/";
		//var maxTendency = linlin(divs*2,2,10,60,30);
		frames = 1024;
		p = path;
		shapesPath.postln;
		{
			d = DecksManager(frames, decks);
			1.wait;
			a = decks.collect({|i| Analyzer(b.server, b, 1024, 42, i)});
			m = MorphSequencers(frames, d.decks, a, ir.buffers, shapesPath);
			//Decks masks
			t_seqs = decks.collect{|i| TendencyMask(100,5,45,45,60) };//maxTendency
			t_seqFades = decks.collect{|i| TendencyMask(100,3,10,10,10) };

			trackSequencers = decks.collect{|i| m.getTracksSequencer(i, t_seqs[i], t_seqFades[i])};
			// Parameters and Reverbs masks
			t_pmorphs = decks.collect{|i| TendencyMask(100,10,12,13,15) };
			paramMorphers = decks.collect{|i| m.getParamsMorpher(i, t_pmorphs[i] )};
			t_rmorphs = decks.collect{|i| TendencyMask(100,10,12,14,18) };
			revMorphers = decks.collect{|i| m.getReverbMorpher(i, t_rmorphs[i] )};
			// Spectral EQ morpher
			t_smorph_time = TendencyMask.new(100, 5, 5, 6, 10);
			t_smorph_fade = TendencyMask.new(100, 5, 5, 6, 10);
			shapeMorpher = m.getShapeMorpher(t_smorph_time, t_smorph_fade);
			// Macro Controls
			this.loadProfiles(path);
			playerMacros = HaringMacros("PlayRate-Stretching Macro-Control",
				objs: m.probabilisticInstruments,
				profiles: playerProfiles,
				morphers: paramMorphers,
				wetControl: false,
				currentProfile: 2
			);
			//  Another type of control for reverb Macros due to IRs
			revMacros = KernelHaringMacros("Reverb Macro-Control",
				objs: m.probabilisticReverbs,
				profiles: revProfiles,
				morphers: revMorphers,
				wetControl: true,
				currentProfile: 8
			);
			timeMacroControllers = ();
			masks = ();
			allMultiMasks = [t_seqs, t_seqFades, t_pmorphs, t_rmorphs];
			allSingleMasks = [[t_smorph_time], [t_smorph_fade]]; // Must be arrays for TimeControls
			allMultiMasks = allMultiMasks ++ allSingleMasks;
			masksTags = ["chunk playback", "same-deck crossfade", "stretch crossfade", "reverb crossfade", "EQ shapes crossfade", "EQ transition curves"];
			allMultiMasks.do{|currentMask, i| masks.put(masksTags[i], currentMask )};
			masksTags.do{|tag| timeMacroControllers.put(tag, TendencyControl(tag, masks[tag]))};
			// Set IR's (by default 0 effects)
			decks.do{|i| Ndef((\Haring_binPassFilter_++i).asSymbol).set(\kernel, ir.buffers[4], \wet, 0.0)};
			mixer = Mixer();
			load = true;
		}.fork;
		this.message("ready!  ");
	}

	*loadProfiles { | path |
		var revPath, playerPath, revData, playerData;
		profilesPath = "%ParameterProfiles/".format(path);
		playerPath = "%defaultPlayerProfiles.scd".format(profilesPath);
		playerProfiles = thisProcess.interpreter.compileFile(playerPath).value;
		// Reverb profiles can only be available kernels, therefore we simply collect our IR bufnums:
		revProfiles = ir.buffers.collect{|bufnum|
			[bufnum,bufnum].reshape(1,2);
		};
	}

	*play {
		m.decks.do{|deck| deck.play }; //instead of lower-level playSilentDecks()
		mixer.play;
		{
			trackSequencers[0].play;
			3.rand.wait;
			trackSequencers[1].play;
			3.rand.wait;
			trackSequencers[2].play;
			3.rand.wait;
			trackSequencers[3].play;
			3.rand.wait;
		}.fork;
		morphBtn.valueAction = 1;
	}

	*stop {
		m.decks.do{|deck| deck.stop };
		a.do{|analizer| analizer.stop };
		trackSequencers.do{|track| track.stop };
		mixer.stop();
		morphBtn.valueAction = 0;
	}

	*pause {
		m.decks.do{|deck| deck.pause };
	}

	*resume {
		m.decks.do{|deck| deck.resume };
	}

	*makeGUI {
		var decorator;
		var numBtns = 7;
		var infoBtn;
		var allocBtn, inspectBtn;
		var width = 544;
		styler = GUIStyler(masterWindow);
		win = styler.getWindow("", Rect(0,0, width,75), border: false);
		win.decorator = FlowLayout(win.bounds);
		decorator = win.decorator;

		styler.getSubtitleText(win, "H.A.R.I.N.G", decorator);

		styler.getButton(win, ">", ">")
		.action_({ |btn|
			if(load){
				if(btn.value > 0){
					this.play;
					//masterWindow.setInnerExtent(544,800);
					masterWindow.setInnerExtent(544,75+50);
					//inspectBtn.valueAction_(1);

					allocBtn.valueAction_(1);
					playerMacros.makeActive;
					revMacros.makeActive;
					this.message("playing..  ");
				} {
					this.stop;
					masterWindow.setInnerExtent(544,75);
					inspectBtn.valueAction_(0);
					allocBtn.valueAction_(0);
					playerMacros.seqBtn.valueAction_(0);
					revMacros.seqBtn.valueAction_(0);
					this.message("stop  ");
				}
			};
		});
		morphBtn = styler.getButton(win, "morph", "morph")
		.action_({ |btn|
			if(btn.value > 0){
				"Morphing active".postln;
				shapeMorpher.play;
			} {
				"Morpher stopped".postln; //7-09-16 THIS CAUSES AN ERROR (IT DOES NOT AFFECT AUDIO BUT DOES NOT STOP MORPHERS)
				shapeMorpher.stop;
			}
		});
		inspectBtn = styler.getButton(win, "inspect", "inspect")
		.action_({ |btn|
			var on = false;
			if(btn.value > 0){
				on = true;
				masterWindow.setInnerExtent(544,800);
				this.callInspectors(on);
			} {
				on = false;
				masterWindow.setInnerExtent(544,75+50);
				this.callInspectors(on);
			}
		});
		allocBtn = styler.getButton(win, "allocator", "allocator")
		.action_({ |btn|
			if(btn.value > 0){
				b.makeInterface(masterWindow);
			} {
				b.close;
			}
		});
		styler.getButton(win, "controls", "controls")
		.action_({ |btn|
			if(btn.value > 0){
				var posX = 544, posY = 0;
				var posY2 = 0;
				masterWindow.setInnerExtent(544 + (293), 800);

				// HERE COMMENTED

				//m.weightedShapes.makeInterface(posX, posY, masterWindow);
				//m.weightedCurves.makeInterface(posX, 15*20, masterWindow);
				playerMacros.makeInterface(posX, 0, masterWindow);
				revMacros.makeInterface(posX , 120, masterWindow);
				masksTags.do{|tag, i|
					timeMacroControllers[tag].makeInterface(posX , (i+1) * 75+190, masterWindow);
				};
				Server.default.fancyMeter(masterWin: masterWindow, posX: 544 , posY: 725);
			} {
				masterWindow.setInnerExtent(544, 800);
				//m.weightedShapes.win.close;
				//m.weightedCurves.win.close;
				playerMacros.close;
				revMacros.close;
				masksTags.do{|tag, i|
					timeMacroControllers[tag].close;
				}
			}
		});
		styler.getButton(win, "depth", "depth")
		.action_({ |btn|
			if(btn.value > 0){
				advancedWindow = Window("", Rect(0,0, 280 * 2.1, 510)).background_(Color.black).alpha_(alpha);
				this.depthRev(true);
				this.depthInst(true);
				advancedWindow.front;
				"Advanced setup, use only if know what you are doing".warn;
			} {
				this.depthRev(false);
				this.depthInst(false);
				advancedWindow.close;
				"-> yeah, I didn't think so...".postln;
			}
		});
		styler.getButton(win, "rec", "rec")
		.action_({ |btn|
			if(btn.value > 0){
				var path;
				Server.default.recChannels = 2;
				Server.default.recHeaderFormat = "aiff";
				Dialog.savePanel({|p|
					{
						if(p[(p.size-1) - 4] != '.') {
							path = p ++ ".aif"
						} {
							path = p;
						};
						Server.default.prepareForRecord(path);
						Server.default.sync;
						Server.default.record;
						this.message("recording..  ");
					}.fork(AppClock);
				}, {"You didn't give me a path".postln});
			}
			{
				Server.default.stopRecording;
				this.message("stop recording  ");
			}
		});
		infoBtn = styler.getButton(win, "info", "info") // Wait a second to define this
		.action_({ |btn|
			var w = 350, h = 600;
			var val = btn.value;
			var posX = 544;
			if(val > 0) {
				infoWindow= Window("", Rect(
					(Window.screenBounds.width / 2) - (w/2),
					Window.screenBounds.height / 2 - (h/2),
					w,h), scroll: true).background_(Color.black).alpha_(alpha);
				this.infoWindowMaker(0,0,infoWindow);
				infoWindow.front;
			} {
				infoWindow.close;
			}
		});
		styler.getButton(win, "credits", "credits")
		.action_({ |btn|
			var w = 350, h = 100;
			if(btn.value > 0) {
				creditsWindow = Window("", Rect(
					(Window.screenBounds.width / 2) - (w/2),
					Window.screenBounds.height / 2 - (h/2),
					w,h)).background_(Color.black).alpha_(alpha);
				this.creditsWindowMaker(0,0,creditsWindow);
				creditsWindow.front;
			} {
				creditsWindow.close;
			}
		});
		styler.getButton(win, "close")
		.action_({ |btn|
			this.close;
		});
		decorator.nextLine;

		styler.getColoredRect(win,Color.gray(0.4, 1));//loaded
		styler.getSubtitleText(win,"loaded",decorator,10,false,align:\left,width:42);
		styler.getColoredRect(win,Color.green(1, 0.75));//playing
		styler.getSubtitleText(win,"playing",decorator,10,false,align:\left,width:42);
		styler.getColoredRect(win,Color.new255(192,191,224));//analysing
		styler.getSubtitleText(win,"analysing",decorator,10,false,align:\left,width:42);
				//decorator.nextLine;

		infoText = styler.getSubtitleText(win,
			"black : nil | cyan : loading | scyan : loaded | white : playing | gray : played | red : analysing",
			decorator,align:\right);

		//decorator.nextLine;


		masterWindow.onClose = { this.close; };
		masterWindow.front;
	}

	*callInspectors{|on = false|
		var xOffset = 0, yOffset = 126;
		var xpos = 0;
		var yPosFactor = 160;
		var numDecks = decks;
		if(on){
			(m.decks.size).do{|i| m.decks[i].morphInspector(xpos + xOffset, i * yPosFactor + yOffset, masterWindow)};
		} {
			(m.decks).do{|deck| deck.inspectorWindow.close };
		}
	}

	*depthRev{ |on = false|
		var yPosVals = Array.series(4, 0, 127);
		var offsetX = 0, offsetY = 0;
		if(on){
			m.probabilisticReverbs.do{|rev, i|
				rev.makeInterface(offsetX, yPosVals[i] + offsetY, advancedWindow);
			};
		} {
			m.probabilisticReverbs.do{|rev| rev.win.close };
		}
	}

	*depthInst { |on = false|
		var yPosVals = Array.series(4, 0, 127);
		var offsetX = styler.ezSizeW * 1.045, offsetY = 0;
		if(on){
			m.probabilisticInstruments.do{|inst, i|
				inst.makeInterface(offsetX , yPosVals[i] + offsetY, advancedWindow)
			};
		} {
			m.probabilisticInstruments.do{|inst| inst.win.close };
		}
	}

	*infoWindowMaker { |posX = 640, posY = 360, masterWin|
		var infoStyler = GUIStyler(masterWin);
		var winWidth = 340;
		var winHeight = 900;
		var decorator, localWin;
		var labelW = 42, descW = 290;
		var sineRange = Pn(Pseries(0, (pi*2)/360, 360),inf).asStream;
		var about;
		localWin = infoStyler.getWindow("",Rect(posX,posY, masterWin.bounds.width, winHeight), true, true);
		localWin.decorator = FlowLayout(localWin.bounds);
		decorator = localWin.decorator;
		infoStyler.getSubtitleText(localWin,"Humanless Audio Recombinator for Infinite Novelty Generation", decorator);
		infoStyler.getSubtitleText(localWin,"About", decorator);
		about = infoStyler.getMultiLineText(localWin,Rect(10,10, winWidth, 110)).string_(
			"I am the Humanless Audio Recombinator for Infinite Novelty Generation. I function as an independent performer that uses tracks provided by the user to create a real-time generated re-composition. This is carried out by automatically splicing the tracks, analysing and selecting them using their Mel-frequency cepstral coefficients and playing them together via a system of spectral morphings controlled by probabilistic curves and tendency masks.Further processing has been constrained to reverberation, bin-stretching and transposition. All the parameters of these controls and effects are tweakable by the user via a set of sliders and buttons, but you may as well enjoy what I come up with without further intervention than the play button..."
		).stringColor_(Color.white);
		//Main panel
		infoStyler.getSizableText(localWin,"Main panel control", winWidth, align: \center, bold: true);
		infoStyler.getSizableText(localWin,"Buttons", winWidth, align: \left, bold: true);
		infoStyler.getSizableText(localWin,">", labelW, align: \center).background_(Color.red);
		infoStyler.getSizableText(localWin,": play/stop", descW, align: \left);
		infoStyler.getSizableText(localWin,"morph", labelW, align: \center).background_(Color.red);
		infoStyler.getSizableText(localWin,": activate the spectral morphing sequencer", descW, align: \left);
		infoStyler.getSizableText(localWin,"inspect", labelW, align: \center).background_(Color.red);
		infoStyler.getSizableText(localWin,": display spectral shapes inspectors", descW, align: \left);
		infoStyler.getSizableText(localWin,"allocator", labelW, align: \center).background_(Color.red);
		infoStyler.getSizableText(localWin,": display analysis/sequencing inspector", descW, align: \left);
		infoStyler.getSizableText(localWin,"controls", labelW, align: \center).background_(Color.red);
		infoStyler.getSizableText(localWin,": display high-level controls", descW, align: \left);
		infoStyler.getSizableText(localWin,"depth", labelW, align: \center).background_(Color.red);
		infoStyler.getSizableText(localWin,": display low-level controls for effects on each deck independently", descW, align: \left);
		infoStyler.getSizableText(localWin,"rec", labelW, align: \center).background_(Color.red);
		infoStyler.getSizableText(localWin,": c'est nes pas un rec button", descW, align: \left);
		//Controls panel
		infoStyler.getSizableText(localWin,"Controls panel", winWidth, align: \center, bold: true);
		infoStyler.getSizableText(localWin,"Weights", winWidth, align: \left, bold: true);
		infoStyler.getSizableText(localWin,"shapes", labelW, align: \center).background_(Color.red);
		infoStyler.getSizableText(localWin,": controls the probabilities for a spectral shape to happen", descW, align: \left);
		infoStyler.getSizableText(localWin,"curves", labelW, align: \center).background_(Color.red);
		infoStyler.getSizableText(localWin,": controls the probabilities for an interpolation curve to happen", descW, align: \left);
		infoStyler.getSizableText(localWin,"Macro Controls", winWidth, align: \left, bold: true);
		infoStyler.getSizableText(localWin,"player", labelW, align: \center).background_(Color.red);
		infoStyler.getSizableText(localWin,": sets seven possible 'profiles' for bin-stretching, from min to max", descW, align: \left);
		infoStyler.getSizableText(localWin,"reverb", labelW, align: \center).background_(Color.red);
		infoStyler.getSizableText(localWin,": sets possible impulse responses for convolution", descW, align: \left);
		infoStyler.getMultiLineText(localWin,Rect(10,10, winWidth, 40)).string_("The 'active' button in both of these controls activate/deactivate independent sequencers to change among values in the range of the selected profiles probabilistically").stringColor_(Color.white);
		infoStyler.getSizableText(localWin,"Time Controls", winWidth, align: \left, bold: true);
		infoStyler.getSizableText(localWin,"wait", labelW, align: \center).background_(Color.red);
		infoStyler.getSizableText(localWin,": control min and max ranges for wait time between tracks", descW, align: \left);
		infoStyler.getSizableText(localWin,"crossfade", labelW, align: \center).background_(Color.red);
		infoStyler.getSizableText(localWin,": control min and max ranges for crossfade time between tracks", descW, align: \left);
		infoStyler.getSizableText(localWin,"stretch", labelW, align: \center).background_(Color.red);
		infoStyler.getSizableText(localWin,": control min and max time ranges for changes of bin-stretching", descW, align: \left);
		infoStyler.getSizableText(localWin,"reverb", labelW, align: \center).background_(Color.red);
		infoStyler.getSizableText(localWin,": control min and max time ranges for changes of reverb wetness", descW, align: \left);
		infoStyler.getSizableText(localWin,"spectrum", labelW, align: \center).background_(Color.red);
		infoStyler.getSizableText(localWin,": control min and max ranges for wait time between new spectral shapes", descW, align: \left);
		infoStyler.getSizableText(localWin,"curve", labelW, align: \center).background_(Color.red);
		infoStyler.getSizableText(localWin,": control min and max ranges for interpolation time between spectral shapes", descW, align: \left);
		infoStyler.getSizableText(localWin,"Signal meters", winWidth, align: \left, bold: true);
		infoStyler.getSizableText(localWin,"Show signal levels in stereo pairs", descW, align: \left);
		// Advanced panel
		infoStyler.getSizableText(localWin,"Advanced panel", winWidth, align: \center, bold: true);
		infoStyler.getSizableText(localWin,"Reverb", labelW, align: \center).background_(Color.red);
		infoStyler.getSizableText(localWin,": Control probability, impulse response and wet ranges independently", descW, align: \left);
		infoStyler.getSizableText(localWin,"Deck", labelW, align: \center).background_(Color.red);
		infoStyler.getSizableText(localWin,": Control probability, bin-stretching and rate ranges independently", descW, align: \left);
		infoStyler.getMultiLineText(localWin,Rect(10,10, winWidth, 40)).string_("NOTICE: Using these controls means that they are dis-attached from the macro-controls, nevertheless will become their slaves again if you change its correspondant parameter").stringColor_(Color.white);
		SkipJack({ about.background = Color(sineRange.next.sin.abs * 0.5,0,0)},0.025, { infoWindow.isClosed });
	}

	// Fix this window so has better and well positioned text...
	*creditsWindowMaker {|posX = 640, posY = 360, masterWin|
		var creditStyler = GUIStyler(masterWin);
		var winHeight = 100;
		var decorator;
		var localWin;
		var fadingWin;
		var redThing;
		var sineRange = Pn(Pseries(0, (pi*2)/360, 360),inf).asStream;
		localWin = creditStyler.getWindow("", Rect(posX, posY, masterWin.bounds.width, winHeight), true);
		localWin.decorator = FlowLayout(localWin.bounds);
		decorator = localWin.decorator;
		redThing = creditStyler.getSizableText(localWin, "I, the H.A.R.I.N.G ough nothing to mere humans!", masterWin.bounds.width,\center, 9, true).background = Color.black;
		creditStyler.getSizableText(localWin, "Still, I would like to mention:",  masterWin.bounds.width,\center);
		creditStyler.getSizableText(localWin, "+ Darien Brito and Andrea Vogrig - Software Architecture and Programming ",  masterWin.bounds.width,\left);
		creditStyler.getSizableText(localWin, "+ Francisco Lopez - General idea/design",  masterWin.bounds.width,\left);
		SkipJack({ redThing.background = Color(sineRange.next.sin.abs,0,0)},0.025, { creditsWindow.isClosed });
	}
}

HaringMacros {
	var <objs, <profiles;
	var title, label, <profSl, <probSl, <wetSl, <legend, <interfaceOn;
	var <currentProbability, <currentProfile, <currentWet, <window;
	var <seqBtn, morphers;
	var <wetControl;
	var <playing;

	*new{ | title, objs, profiles, morphers, wetControl = false ,currentProfile|
		^super.new.init( title, objs, profiles, morphers, wetControl,currentProfile )
	}

	init { |title_, objs_, profiles_, morphers_, wetControl_ ,currentProfile_|
		title = title_;
		objs = objs_;
		profiles = profiles_;
		wetControl = wetControl_;
		interfaceOn = false;
		currentProbability = 25;
		currentWet = 0.5;
		currentProfile = currentProfile_;
		morphers = morphers_;
		playing = false;
		this.setProbability(currentProbability);
		if(wetControl) {
			this.setDryWet(currentWet);
		};
		this.setProfile(currentProfile);
	}

	close {
		window.close;
	}

	setProfile { |val|
		var value = val.round();
		if(interfaceOn) {
			profSl.valueAction_(value);
		} {
			currentProfile = value;
			objs.do{|obj| obj.loadSet(profiles[currentProfile])}; // By default no effects
		}
	}

	setProbability{|val|
		if(interfaceOn) {
			probSl.valueAction_(val);
		} {
			currentProbability = val;
			objs.do{|obj| obj.setProbability(currentProbability)};
		}
	}

	setDryWet{|val|
		if(interfaceOn) {
			wetSl.valueAction_(val);
		} {
			currentWet = val;
			//	objs.do{|obj| obj.setDryWet(currentWet)};
		}
	}

	makeActive{
		if(playing.not){
			playing = true;
			morphers.do{|morpher| morpher.play}
		}
	}

	makeInterface { |posX = 0, posY = 0, masterWin|
		var styler = GUIStyler(masterWin);
		var numSliders = 2;
		var winWidth =  styler.ezSizeW + (styler.gadgetWidth/2);
		var winHeight = (styler.gadgetHeight * 7.5);
		var decorator;
		var legendLabels = [
			"zero effects",
			"min range",
			"mid-low range",
			"mid range",
			"mid-hi range",
			"hi range",
			"max range"
		];
		window = styler.getWindow("", Rect(posX, posY, winWidth, winHeight));
		interfaceOn = true;
		window.decorator = FlowLayout(window.bounds);
		decorator = window.decorator;
		label = styler.getSubtitleText(window, title, decorator);
		probSl = styler.getEZSlider(window, "probability", ControlSpec(0,100,\lin, 0.1),\horz);
		profSl = styler.getEZSlider(window, "profile", ControlSpec(0,6,\lin,1,0),\horz);
		if(wetControl) {
			wetSl = styler.getEZSlider(window, "wet", ControlSpec(0.0, 1.0,\lin, 0.01),\horz);
			wetSl.action = {|sl|
				var val = sl.value;
				currentWet = val;
				//	objs.do{|obj| obj.setDryWet(currentWet) };
			};
		};

		decorator.nextLine;
		legend = styler.getSubtitleText(window, "Minimum", decorator);
		styler.getSizableText(window, "Activate probabilistic parameter sequencer ----->",
			styler.ezSizeW - (styler.gadgetWidth * 2)
		); //Some empty space
		seqBtn = styler.getButton(window, "active", "x");

		/*
		Task proxies, they accept:.set(\waitLo, aVal, \waitHi, aVal) while running
		for changing rrand boundaries for timing
		*/
		seqBtn.action = {|btn|
			if(btn.value > 0){
				playing = true;
				morphers.do{ |morpher| morpher.play };
			} {
				playing = false;
				morphers.do{ |morpher| morpher.stop };
			}
		};

		probSl.action = {|sl|
			var val = sl.value;
			//"Macros prob %".format(val).postln;
			currentProbability = val;
			objs.do{|obj| obj.setProbability(currentProbability)}
		};

		profSl.action = {|sl|
			var val = sl.value;
			currentProfile = val;
			objs.do{|obj| obj.loadSet(profiles[currentProfile])};
			legend.string_(legendLabels[val]);
		};

		//Set to current
		this.setProbability(currentProbability);
		if(wetControl) {
			this.setDryWet(currentWet);
		};
		this.setProfile(currentProfile);

		if(playing) {
			seqBtn.value = 1;
		} {
			seqBtn.valueAction = 0;
		};
		//window.front;
		masterWin.onClose = { interfaceOn = false }
	}
}

KernelHaringMacros : HaringMacros {
	makeInterface { |posX = 0, posY = 0, masterWin|
		var styler = GUIStyler(masterWin);
		var numSliders = 2;
		var winWidth =  styler.ezSizeW + (styler.gadgetWidth/2);
		var winHeight = (styler.gadgetHeight * 7.5);
		var decorator;
		var legendLabels = [
			"Cathedral",
			"Deep Well",
			"Drum Room",
			"Full-size Auditorium",
			"Large Concert Hall",
			"Large Dome",
			"Large Metallic Cylinder",
			"Large Tank",
			"Mid-Size Auditorium",
			"Mid-Size Room",
			"Mixed Impulses",
			"Octagonal Room",
			"Open Air Stadium",
			"Open Air Theatre",
			"Small Room",
			"Space Capsule",
			"Theatre"
		];
		window = styler.getWindow("", Rect(posX, posY, winWidth, winHeight));
		interfaceOn = true;
		window.decorator = FlowLayout(window.bounds);
		decorator = window.decorator;
		label = styler.getSubtitleText(window, title, decorator);
		probSl = styler.getEZSlider(window, "probability", ControlSpec(0,100,\lin, 0.1),\horz);
		profSl = styler.getEZSlider(window, "profile", ControlSpec(0, legendLabels.size - 1, \lin,1,0),\horz);
		if(wetControl) {
			wetSl = styler.getEZSlider(window, "wet", ControlSpec(0.0, 1.0,\lin, 0.01),\horz);
			wetSl.action = {|sl|
				var val = sl.value;
				currentWet = val;
				objs.do{|obj| obj.setDryWet(currentWet) };
			};
		};
		decorator.nextLine;
		legend = styler.getSubtitleText(window, "Minimum", decorator);
		styler.getSizableText(window, "Activate probabilistic parameter sequencer ----->",
			styler.ezSizeW - (styler.gadgetWidth * 2)
		);
		seqBtn = styler.getButton(window, "active", "x");

		seqBtn.action = {|btn|
			if(btn.value > 0){
				playing = true;
				morphers.do{ |morpher| morpher.play };
			} {
				playing = false;
				morphers.do{ |morpher| morpher.stop };
			}
		};

		probSl.action = {|sl|
			var val = sl.value;
			//"Macros prob %".format(val).postln;
			currentProbability = val;
			objs.do{|obj| obj.setProbability(currentProbability)}
		};

		profSl.action = {|sl|
			var val = sl.value;
			currentProfile = val;
			objs.do{|obj| obj.loadSet(profiles[currentProfile])};
			legend.string_(legendLabels[val]);
		};

		//Set to current
		this.setProbability(currentProbability);
		if(wetControl) {
			this.setDryWet(currentWet);
		};
		this.setProfile(currentProfile);

		if(playing) {
			seqBtn.value = 1;
		} {
			seqBtn.valueAction = 0;
		};
		//window.front;
		masterWin.onClose = { interfaceOn = false }
	}

}