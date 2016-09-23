////////////////////////////////////////////////////////////////////////////////////////////////////
//  Allocator, buffers allocation management
//
// * Allocation / deallocation of Buffers
// * tracks cue
//
//  Splitter
// * execution of PDC to split audio files Unix command are used
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
Allocator{
	var <server,<>masterpath,<>trackpath,<division,<dynamic_allocation,<maxBuffersAtTime,<ext_callback;
	// Buffer states
	// 0     loading
	// 1     loaded
	// 2     playing
	// 3     played
	// 4     analising
	classvar buffersStates,seq,splitter;
	var <cue,<buffers;
	var <totalBuffers;
	var <loaded;
	var <pdcpath,<outpath,<offest;
	var <tmpPath;


	var <counter,<current;

	*initClass {
		buffersStates = IdentityDictionary.new;
	}

	*new{|server,masterpath,trackpath,division,dynamic_allocation=false,maxBuffersAtTime,ext_callback|
		server = server ? Server.default;
		^super.newCopyArgs(server,masterpath,trackpath,division,dynamic_allocation,maxBuffersAtTime,ext_callback).init();
	}

	init{
		if(File.exists(masterpath).not) {
			Error("The master directory doesn't exist.").throw;
		}{
			tmpPath = "/tmp/Haring/";
			counter = 0; // reset counter
			loaded = 0;
			current = 0;

			offest = 1000;
			pdcpath = (masterpath ++ "PDC"); // pdc path
			outpath = PathName.new(trackpath);//(masterpath ++ "Tracks/");
			outpath = outpath.fullPath;

			if(division != "none"){
				splitter = Splitter(pdcpath,division,outpath,{|res|
					if(res == 0){
						if(dynamic_allocation == true){
							"preload".postln;
							this.preload;
						}{
							"normal".postln;
							this.load_splitted;
						};
				    }{
						this.callback(res)
					};
				});

				//if(File.exists(tmpPath ++ "0/").not){
					splitter.split();  // call PDC to split all the tracks
				/*}{
					if(dynamic_allocation == true){
						"preload".postln;
						this.preload;
					}{
						"normal".postln;
						this.load_splitted;
					};
				};*/
			}{//none
				if(dynamic_allocation == true){
					this.preload;
				}{
					this.load_noSplitted;
				};
			};


		};
	}

	callback{|res|
		if(ext_callback != nil){
			ext_callback.value(res);
		}
	}

	count_dir{
		var i = 0;
		while({File.exists(tmpPath ++ i ++ "/")},
			{
			i = i + 1;
		});
		^i
	}

	load_noSplitted{
		var files;

		files = SoundFile.collect(outpath++"*");
		if((files != nil) && (files.size > 0)){
			totalBuffers = files.size;
			cue = Array.series(totalBuffers,0,1);
			seq = SeQ.new(0,80,totalBuffers);
			files.collect { | p , index|
					var buf = Buffer.read(server, p.path,action:{|b|
						this.set_state(b.bufnum,1); // set state 1 = loaded
						loaded = loaded + 1;
						if(loaded == totalBuffers){
							this.callback(0);
						}
					});

					if(buf == nil){
						"buffers nil found".postln;
					}{
						this.set_state(buf.bufnum,0); // set state 0 = loading
					};

			};
		}{
				"no files founds".postln;
				this.callback(2);

		};
	}


	load_splitted{
		var files,rand;
		totalBuffers = this.count_dir;

		cue = Array.series(totalBuffers,0,1);
		seq = SeQ.new(0,80,totalBuffers);

		totalBuffers.do{|i|
			i.postln;
			files = SoundFile.collect(tmpPath++i++"/"++ "*");
			if((files != nil) && (files.size > 0)){
				rand = files.size.rand; // choose random chunk for each tracks
				files.collect { | p , index|
					if(index == rand){
						var buf = Buffer.read(server, p.path,action:{|b|
							this.set_state(b.bufnum,1); // set state 1 = loaded
							loaded = loaded + 1;
							if(loaded == totalBuffers){
								this.callback(0);
							}
						});

						if(buf == nil){
							"buffers nil found".postln;
						}{
							this.set_state(buf.bufnum,0); // set state 0 = loading
						};

					};
				};
			}{
				"no files founds".postln;
				this.callback(2);
			};
		};
	}



	set_state{|buf,state|
		buffersStates.put(buf.asSymbol,state); // set state
		if(dynamic_allocation == true){
			seq.update(buf.asInteger - offest,state);
		}{
			seq.update(buf.asInteger,state);
	    };
	}

	next_static{

		if(cue.size == 0 ){
			"reset-----------------------------------------------".postln;
			seq.reset;
			counter = 0;
			cue = Array.series(totalBuffers,0,1);
		};

		counter = counter + 1;
		current = cue.scramble.choose;
		cue.remove(current);

		^current;
	}

	next_analyzer_static{
		var res = List();
		"next analyzer".postln;
		2.do{
			var tmp = this.next_static;
			this.set_state(tmp,4);
			res.add(tmp);
		};
		^res;
	}

	add_cue{|buf|
		cue.add(buf);
	}

	makeInterface{|win|
		seq.init_view(win);
	}

	close{
		seq.close;
	}

	//--------------------------------------------------------------------

	preload{
		//preload the buffers
		totalBuffers = this.count_dir;

		cue = Array.series(totalBuffers,0,1);
		seq = SeQ.new(0,80,totalBuffers);

		// load the firts buffers
		maxBuffersAtTime.do{|b|
			this.loadNext_dynamic;
		};

		this.callback(0);

	}

	loadbuffer{|path,index|
		Buffer.read(server, path,action:{|b|
			buffersStates.put(b.bufnum.asSymbol,1); // set state 1 = loaded
			seq.update(index-offest,1);
		    "loaded %".format(index).postln;
		},bufnum:index);
	}

	test_buffer{|bufnum|
		var res = 0;
		var d1,d2,d3,d4;
		"test buffer %".format(bufnum).postln;
		if(bufnum != nil){
			d1 = Ndef(\Haring_binPassFilter_0).get(\inBuffer);
			d2 = Ndef(\Haring_binPassFilter_1).get(\inBuffer);
			d3 = Ndef(\Haring_binPassFilter_2).get(\inBuffer);
			d4 = Ndef(\Haring_binPassFilter_3).get(\inBuffer);
			if(bufnum == d1){ res = 1 }{0};
			if(bufnum == d2){ res = 2 }{0};
			if(bufnum == d3){ res = 3 }{0};
			if(bufnum == d4){ res = 4 }{0};
		}{
			"error Buffer nil".postln;
			res = 5;
		};
		^res;
	}

	loadNext_dynamic{
		var files,rand,buf;
			this.nextFiles; // point to next folder
			files = SoundFile.collect(tmpPath++current++"/"++ "*");
			rand = files.size.rand;
			files.collect { | p , index|
				if(index == rand){
					buf = this.loadbuffer(p.path,current+offest);
				};
			};
			^buf;
	}

	next{
		if(dynamic_allocation == true){
			^this.next_analyzer_dynamic;
		}{
			^this.next_analyzer_static;
		};

	}


	next_analyzer_dynamic{
		var bufnum,track;
		var res = Array(2);


		2.do{

			this.loadNext_dynamic;

			bufnum = buffersStates.findKeyForValue(1); // search for loaded buffer
			if(bufnum != nil){

				res.add(bufnum.asInteger);

				buffersStates.put(bufnum.asSymbol,4); // set state 4 = analysing

				seq.update(bufnum.asInteger - offest,4);

				bufnum = nil;

			}{
				"analysis: loaded buffers not founds.".postln;
				^nil;
			};

		}

		^res;
	}

	nextFiles{
		//counter = counter + 1; // increment the track counter
		counter = counter +1;
		current = cue.scramble.choose;
		cue.remove(current);

		if(cue.size <= 8){
			"reset-----------------------------------------------".postln;
			seq.reset;
			counter = 0;
			cue = Array.series(totalBuffers,0,1);
		};
	}

	getBufferAt{|index| ^server.cachedBufferAt(index)}

	freeAllBuffers{ server.clearServerCaches(server)}

	freeBufferAt{|index| "freeBuff %".format(index).postln; server.cachedBufferAt(index).free}

	printAllBuffers{ server.cachedBuffersDo({|buf| buf.postln })}

	printStates{
		buffersStates.postln;
	}

	findandFreeBuffer{|buf,id|
		var res=0,tmp;
		//bufnum = buffersStates.findKeyForValue(3);
		res = this.test_buffer(buf);
		if(res == 0){
			tmp = this.getBufferAt(buf.asInteger);
			if(tmp != nil){
				"find&free buffer % from : %".format(buf,id).postln;
				buffersStates.removeAt(buf.asSymbol);
				this.freeBufferAt(buf.asInteger); // free the buffer
			}{"buffer nil".postln};
		}{
			"error while cleaning buffer %".format(res).postln;
		};
	}

}

Splitter{

	var <pdcpath,<division,<outpath,<ext_callback;
	var <tmpPath;

	*new{|pdcpath,division="1.5",outpath,ext_callback|
		^super.newCopyArgs(pdcpath,division,outpath,ext_callback);
	}

	split{
		tmpPath = "/tmp/Haring/";
		"PDC started".postln;
		this.run("-d",division); // execute PDC script and wait for callback
	}

	run{|program, params|
		var cmd,sep = "\"";
		cmd = pdcpath++" "++program++" "++outpath.quote++" "++tmpPath.quote++" "++params;
		cmd.postln;
		cmd.unixCmd({|res,pid|
			"PDC ended.".postln;
			//totalBuffers = res.asInteger;
			if(ext_callback != nil){
				ext_callback.value(res.asInteger);
			};
		});
	}

}

