////////////////////////////////////////////////////////////////////////////////////////////////////
//
// Haring utilites
//
// * Wrapper for functions and data managers
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

DecksManager {
	var <frames, <names, <decks, <deckNum;
	var <interpolationTypes;
	var <instruments, <reverbs;

	*new {| frames = 512, deckNum = 4 |
		^super.new.init(frames, deckNum)
	}

	init{ |frames_, deckNum_|
		frames = frames_;
		names = List();
		decks = ();
		deckNum = deckNum_;
		interpolationTypes = [
			\lin, \exp, \explin,\lincurve,\curvelin, \ramp,
			\bilin,\biexp,\lcurve,\scurve
		];
		this.createDecks(deckNum);
		this.setEQBufs;
		//this.playSilentDecks; // We handle this one level up
	}

	createDecks { | num |
		"Loading decks...".postln;
		num.do{|i|
			var id = i;
			var deck = BinPass2(frames, id);
			decks.put(id, deck);
		}
	}

	setEQBufs {
		decks.keysValuesDo{|key, deck|
			var instrumentName = (\Haring_binPassFilter_++key).asSymbol;
			Ndef(instrumentName).set(\binFilter, deck.eqBuffer);
		}
	}

	playSilentDecks {
		decks.do{|deck| deck.play }
	}

}

ShapeGenerator {
	var <frames, <shapes, <shape, <shapeTags, <curveTags, <shapePath;

	*new{|frames, shapePath|
		^super.new.init(frames, shapePath)
	}

	init {|frames_, shapePath_|
		frames = frames_ / 4; // Actual amount of useful bins
		shapePath = shapePath_;
		shape = ();
		shapeTags =List();
/*		[
			//\lin, \exp, \explin, \lincurve, \curvelin, \ramp, \bilin, \biexp,
			//\lcurve, \scurve, \rectLow,
			\brownian, \white, \betaLow, \betaHigh, \sine,
			\cos, \gauss, \rectHigh, \even, \odd
		].collect{|name| shapeTags.add(name)};*/
		curveTags = [
			\lin, \exp, \explin,\lincurve,\curvelin, \ramp,
			\bilin,\biexp,\lcurve,\scurve
		];
		//this.createShapes;
		this.loadFromFile(shapePath);
	}

	loadFromFile { | path |
		var paths = (path ++ "*").pathMatch;
		var names = paths.collect{|shape, i| i.asSymbol };
		paths.do{|p, i|
			var num,delta;
			var file = File(p.asString, "r");
			var fileString = file.readAllString;
			fileString = fileString.split($\n);
			num = fileString.collect{|val, i| val.asFloat};
			num = num.resize(256);
			shape.put((\preset ++ names[i]).asSymbol, num);
			shapeTags.add((\preset ++ i).asSymbol);
		};
	}

	getShapes {
		^shape
	}

	createShapes {
		var mapArray =  Pseries(0, 1/frames, frames).asStream.nextN(frames);
		var mapLo =  0;
		var expLo = 0.0001;
		var rampData = mapArray.normalize(-1 , 1);
		var curveLData = mapArray.normalize(-10 , 10);
		var normalizedData = mapArray.normalize();
		var gaussData =  mapArray.normalize(curve * -1, curve);
		var centerA = 1.0.rand;
		var centerB = 1.0.rand;
		var curve = 4;
		shape[\lin] = { mapArray.linlin(mapLo, 1.0, 0.0, 1) }.value;
		shape[\exp] = { mapArray.linexp(mapLo, 1.0, expLo, 1)}.value;
		shape[\explin] = { mapArray.explin(expLo, 1.0,0.0,1) }.value;
		shape[\lincurve] = { mapArray.lincurve(mapLo, 1.0,expLo,1,curve) }.value;
		shape[\curvelin] = { mapArray.curvelin(mapLo, 1.0,0.0,1,curve) }.value;
		shape[\ramp] = { rampData.collect { |num| num.ramp } }.value;
		shape[\bilin] = { mapArray.bilin(centerA,mapLo, 1.0,centerB, 0.0, 1)}.value;
		shape[\biexp] = { mapArray.biexp(centerA,expLo, 1.0,centerB, expLo,1.0)}.value;
		shape[\lcurve] = { curveLData.collect{|num| num.lcurve } }.value;
		shape[\scurve] = { normalizedData.collect {|num| num.scurve } }.value;
		shape[\brownian] = Pbrown(0.0, 1.0, 1/frames, frames).asStream.nextN(frames);
		shape[\white] = Pwhite(0.0, 1.0, frames).asStream.nextN(frames);
		shape[\betaLow] = Pbeta(0.0, 1.0, 0.25, 0.75, frames).asStream.nextN(frames);
		shape[\betaHigh] = Pbeta(0.0, 1.0, 0.75, 0.25, frames).asStream.nextN(frames);
		shape[\sine] = Pseries(0, (pi*2)/frames, frames).asStream.nextN(frames).sin.normalize;
		shape[\cos] = Pseries(0, (pi*2)/frames, frames).asStream.nextN(frames).cos.normalize;
		shape[\gauss] = { gaussData.collect{ |num| num.gaussCurve } }.value;
		shape[\rectHigh] = mapArray.collect{|i| if(i < 0.5) {0.1} {0.9} };
		shape[\rectLow] = shape[\rectHigh].reverse;
		shape[\even] = mapArray.collect{|item, i| if(i.even) {1.0} {0.0} };
		shape[\odd] = mapArray.collect{|item, i| if(i.odd) {1.0} {0.0} };
	}
}

MorphSequencers {
	var frames, shapesPath;
	var <shapeGen, <shapes, <shapeTags, <curveTags;
	var <weightedShapes, <weightedCurves;
	var <probabilisticInstruments, <probabilisticReverbs;
	var <decks, <instruments, <reverbs,<analyzer;
	var server;
	var <ranges;
	var <irBuffers;

	*new { | frames = 512, decks, analyzer, irBuffers, shapesPath|
		^super.new.init(frames, decks, analyzer, irBuffers, shapesPath);
	}

	init {|frames_, decks_, analyzer_, irBuffers_, shapesPath_|
		frames = frames_;
		decks = decks_;
		analyzer = analyzer_;
		irBuffers = irBuffers_;
		shapesPath = shapesPath_;
		server = Server.default;
		shapeGen = ShapeGenerator(frames, shapesPath);
		shapeTags = shapeGen.shapeTags;
		curveTags = shapeGen.curveTags;
		shapes = shapeGen.getShapes;
		instruments = (decks.size).collect{|i| decks[i].instrumentName };
		// Shaper
		this.createWeightGenerators;
		this.createProbabilisticGenerators;
	}

	createWeightGenerators {
		weightedShapes = WeightsGenerator(shapeTags, "EQ Shapes");
		weightedCurves = WeightsGenerator(curveTags, "EQ Transition curves");
		// weightedShapes.set([ 0.84, 0.92, 0.92, 0.96, 0.91, 0.96, 0.92, 0.94, 1, 0.88 ]);
		weightedShapes.set([ 0.84, 0.92, 0.92, 0.96, 0.91, 0.96, 0.92, 0.94, 1, 0.88, 0.92, 0.96, 0.91, 0.96, 0.92, 0.94, 1, 0.88,1 ,1]);
		weightedCurves.setAll(0.01);// HERE NEW
	}
	createProbabilisticGenerators {
		probabilisticInstruments = instruments.collect{|inst, i|
			ProbabilityGenerator(Ndef(inst),[
				\stretch, [0.1, 0.8], // TO DO: create more profiles
				\rate, [0.6, 0.9]
			], "Deck %".format(i+1));
		};
		probabilisticReverbs = instruments.collect{|inst, i|
			ProbabilityGenerator(Ndef(inst), [
				\kernel, [irBuffers.first, irBuffers.last, \lin, 1], // step of 1 for bufs
				\wet, [0.0, 1.0]
			], "Rev %".format(i+1));
		}
	}

	fadeTo { | deckID, fadeTime , newBuffer |
		"fading deck %".format(deckID).postln;
		Ndef(instruments[deckID]).fadeTime = fadeTime;

		//set played callback
		this.set_played(deckID, fadeTime);

		Ndef(instruments[deckID]).xset(\inBuffer, newBuffer, \amp, 0.3); // Scaled to 1/4
	}

	set_played{|id,time|
		var currentBuf = Ndef(instruments[id]).get(\inBuffer);
			if(currentBuf != nil){
			if(currentBuf > 0){
				{
				   time.wait;
					"set_played bufnum: % from: %".format(currentBuf,id).postln;
					analyzer[id].freeBuffer(currentBuf,id,time);
				}.fork;
			};
			};
	}

	/*
	Sequencers

	Done with TaskProxies to avoid any possible name conflicts
	while mantaining the possibility to alter parameters
	on a higher level via the .set method
	*/

	getShapeMorpher {|timeMask, fadeMask|
		var task = TaskProxy({ |ev|
			var val;
			inf.do{|i|
				if(i > 3) {
					val = rrand(0,3);
				}{
					val = i.asInt;
				};
				decks[val].morphTo(
					end: shapes[shapeTags.wchoose(weightedShapes.weights)],
					time: ev.time,
					type: curveTags.wchoose(weightedCurves.weights)
				);
				ev.waitTime.wait;
				task.set(
					\time, fadeMask.next,
					\waitTime, timeMask.next,
				);
				ev.time.postln;
			}
		}).quant_(0);
		task.set(
			\time, fadeMask.next,
			\waitTime, timeMask.next,
		);
		^task;
	}

	getParamsMorpher {|deckID, mask|
		var task = TaskProxy({|ev|
			inf.do{
				probabilisticInstruments[deckID].setParameters;
				ev.waitTime.wait;
				task.set(
					\waitTime, mask.next
				);
			}
		}).quant_(0);
		task.set(
			\waitTime, mask.next
		);
		^task;
	}

	getReverbMorpher {|deckID, mask|
		var task = TaskProxy({|ev|
			inf.do{
				probabilisticReverbs[deckID].setParameters;
				ev.waitTime.wait;
				task.set(
					\waitTime, mask.next
				);
			}
		}).quant_(0);
		task.set(
			\waitTime, mask.next
		);
		^task;
	}

	getTracksSequencer {| id, maskWait, maskFade|
		var task;
		task = TaskProxy({|ev|
			inf.do{|i|
				var bufs;
				"start analyzer: %".format(id).postln;
				analyzer[id].run(ev.waitTime/1.1,{|result|
					bufs = result;
					"anal %".format(id).postln;
				});
				ev.waitTime.wait;
				"Deck % ----------".format(id).postln;
				"Current fadeTime: %".format(ev.fadeTime).postln;
				"Current waitTime: %".format(ev.waitTime).postln;
				"Curren buffer: %".format(bufs).postln;
				"-----------------".postln;
				this.fadeTo(id, ev.fadeTime,bufs);
			}
		}).quant_(0);
		task.set(
			\fadeTime,maskFade.next,
			\waitTime,maskWait.next;
		);
		^task;
	}

}