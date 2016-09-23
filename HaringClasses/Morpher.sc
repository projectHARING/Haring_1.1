// Copyright (C) <2016>
//
// by Darien Brito
// http://www.darienbrito.com
// based on key concepts from Alberto de Campo's "CloudGenMini"
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

Morpher {
	var <morphTime, <stepsPerSec, <curve;
	var <data, <current, <isPlaying, <id;
	var <interpolationTypes;

	*new{ | morphTime = 2, stepsPerSec = 20, curve = 4, id = 0 |
		^super.new.init(morphTime, stepsPerSec, curve, id);
	}

	init { |morphTime_, stepsPerSec_, curve_, id_|
		morphTime = morphTime_;
		stepsPerSec =stepsPerSec_;
		curve = curve_;
		id = id_;
		data = VarInterpolationData(morphTime, stepsPerSec, curve);
		isPlaying = false;
		interpolationTypes = data.getInterpolationTypes;
	}

	morphTask { | from, target, time, type |
		^TaskProxy({
			var blendVal;
			var numSteps = time * stepsPerSec;
			var tmp;
			morphTime = time;
			data.fillInterpolationSets(time);
			data.setAltered;// Replace the new data set with the one from plotter, if any.
			if(target.notNil) {
				"morphing %...".format(id).postln;
				numSteps.do{|i|
					blendVal = (i + 1) / numSteps;
					tmp = data.getInterpolationData(type)[i];
					if(tmp != nil) {
						current = from.blend(target, blendVal * tmp);
					};
					(1/stepsPerSec).wait;
				};
				current = target.copy;
				isPlaying = false;
				"morhper: % done".format(id).postln;
			};
		}).quant_(0);
	}

	morphTo { |start, end, time = 5, type = \lin|
		var morpher = this.morphTask(start, end, time, type);
		current = start;
		isPlaying = true;
		morpher.stop.play; // To fix overlaps
	}

	getVarInterpolationData { |time|
		^data
	}

	isActive {
		^isPlaying
	}

	getCurrent{
		^current
	}

	getTime {
		^morphTime
	}

	getStepsPerSec {
		^stepsPerSec
	}

	getPlotter {|type, time|
		data.fillInterpolationSets(time);
		data.makePlotter(type)
	}

}