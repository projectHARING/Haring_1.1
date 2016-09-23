////////////////////////////////////////////////////////////////////////////////////////////////////
//
// Ir buffers management
//
// * Allocation / deallocation of Buffers
//
// Copyright (C) <2016>
//
// by Andrea Vogrig
// http://andreavogrig.com
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
Ir {
	var <server,<>masterpath,<callback;
	var <irpath;
	var <buffers;

	*new{|server,masterpath,callback|
		^super.newCopyArgs(server,masterpath,callback).load();
	}

	load{
		var files;
		var loaded = 0;
		buffers = List();
		"ir loading..".postln;
		irpath = (masterpath ++ "Ir/"); // pdc path
		files = SoundFile.collect(irpath ++ "*");
		files.collect { | p , index|
			var buf = Buffer.readChannel(
				server,
				p.path,
				channels:[0,1],
				action:
				{
					if(loaded == (files.size-1)){
						callback.value;
			        };
					loaded = loaded+1;
			    });
			buffers.add(buf.bufnum);
		};
		"ir loaded".postln;
	}
}
