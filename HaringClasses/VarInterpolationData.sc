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

VarInterpolationData {
	var <morphTypes, <curve, morphTime, stepsPerSec, <plotter;
	var <interpolationData, alteredData, alteredType, alteredFlag;

	*new { |morphTime = 2, stepsPerSec = 20, curve = 4|
		^super.new.init(morphTime, stepsPerSec, curve);
	}

	init { |morphTime_, stepsPerSec_, curve_|
		morphTime = morphTime_;
		stepsPerSec = stepsPerSec_;
		curve = curve_;
		morphTypes =
		[
			\lin, \exp, \explin,\lincurve,\curvelin, \ramp,
			\bilin,\biexp,\lcurve,\scurve,\custom // \gausscurve (creates trouble)
		];
		interpolationData = ();
		alteredFlag = false;
		this.fillInterpolationSets(morphTime);
	}

	fillInterpolationSets { | time |
		var numSteps = time * stepsPerSec;
		var mapArray =  Pseries(1/numSteps,1/stepsPerSec, numSteps).asStream.nextN(numSteps);
		var mapLo =  1 / numSteps;
		var expLo = 0.0001;
		var rampData = mapArray.normalize(-1 , 1);
		var curveLData = mapArray.normalize(-10 , 10);
		var normalizedData = mapArray.normalize();
		var gaussData =  mapArray.normalize(curve * -1, curve);
		var centerA = 1.0.rand;
		var centerB = 1.0.rand;

		interpolationData[\lin] = { mapArray.linlin(mapLo, time, 0.0, 1) }.value;
		interpolationData[\exp] = { mapArray.linexp(mapLo, time, expLo, 1)}.value;
		interpolationData[\explin] = { mapArray.explin(expLo, time, mapLo,1) }.value;
		interpolationData[\lincurve] = { mapArray.lincurve(mapLo, time,expLo,1,curve) }.value;
		interpolationData[\curvelin] = { mapArray.curvelin(mapLo, time,0.0,1,curve) }.value;
		interpolationData[\ramp] = { rampData.collect { |num| num.ramp } }.value;
		interpolationData[\bilin] = { mapArray.bilin(centerA,mapLo, time,centerB, 0.0, 1)}.value;
		interpolationData[\biexp] = { mapArray.biexp(centerA,expLo, time,centerB, expLo,1.0)}.value;
		interpolationData[\lcurve] = { curveLData.collect{|num| num.lcurve } }.value;
		interpolationData[\scurve] = { normalizedData.collect {|num| num.scurve } }.value;
		interpolationData[\gausscurve] = { gaussData.collect{ |num| num.gaussCurve } }.value;
		interpolationData[\custom] = { mapArray.linlin(mapLo, time, 0.0, 1)  }.value;
	}

	makePlotter { |type|
		var originY = Window.screenBounds.height;
		if(type != nil) {
			plotter = Plotter("Mapping: "++type,
				bounds: Rect(0, originY - 500,415, 300))
			.plotMode_(\plines)
			.value_(interpolationData[type].copy); //Copy (non-destructive)
			plotter.setProperties(
				//\plotColor, (10..0).normalize(0.1, 1).collect { |i| Color.rand(i) },
				//\backgroundColor, Color.black ,
				//\gridColorX, Color.yellow(0.5),
				//\gridColorY, Color.yellow(0.5),
				//\fontColor, Color(0.5, 1, 0)
			)
			.editMode_(true)
			.editFunc_({
				|plotter|
				alteredFlag = true;
				alteredData = plotter.value; // Store altered
				alteredType = type;
				//interpolationData[type] = plotter.value; //Update, works only locally
			});
		} { "Please provide an interpolation type first".postln }
	}

	setAltered {
		if(alteredFlag) { //Transform only if altered
			interpolationData[alteredType] = alteredData;
			alteredFlag = false;
		}
	}

	getInterpolationData { |type|
		^interpolationData[type];
	}

	getInterpolationTypes {
		^morphTypes
	}
}