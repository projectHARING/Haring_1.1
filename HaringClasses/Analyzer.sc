////////////////////////////////////////////////////////////////////////////////////////////////////
//
// Haring
//
// * Analyzer class
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
// along with this program. If not, see <http://www.gnu.org/licenses/>.
Analyzer{
	var <server,<allocator,<melSize,<id,<>analysis,<>result;
	var <task;

	var buf_in,buf_a,buf_b,
	    bus_in,bus_a,bus_b,
	    bufA,bufB;

	*new { |server, allocator, frames,melSize=13,id=0,analysis=false|
		server = server ? Server.default;
		^super.newCopyArgs(server,allocator,melSize,id).init(frames);
	}

	init{|frames|

		analysis = false;

		buf_in = Buffer.alloc(server, frames, 1);
		buf_a = Buffer.alloc(server, frames, 1);
		buf_b = Buffer.alloc(server, frames, 1);

		bus_in = Bus.control(server, melSize);
		bus_a = Bus.control(server, melSize);
		bus_b = Bus.control(server, melSize);

		Ndef((\anal_++id).asSymbol).stop;
		Ndef((\anal_++id).asSymbol,{|in,bufA,bufB,out1,out2,out3|
			var sig_in,sig_a,sig_b,
			fft_in,fft_a,fft_b,
			res_in,res_a,res_b;

			sig_in = In.ar(in);
			sig_a = PlayBuf.ar(2,bufA,BufRateScale.kr(bufA),1,0,0);
			sig_b = PlayBuf.ar(2,bufB,BufRateScale.kr(bufB),1,0,0);

			fft_in = FFT(buf_in,sig_in);
			fft_a = FFT(buf_a,sig_a);
			fft_b = FFT(buf_b,sig_b);

			res_in = MFCC.kr(fft_in, melSize);
			res_a = MFCC.kr(fft_a, melSize);
			res_b = MFCC.kr(fft_b, melSize);

			Out.kr(out1,res_in);
			Out.kr(out2,res_a);
			Out.kr(out3,res_b);

		}).quant_(0);

	}

	stop {
		"Analizer stopped".postln;
		task.stop;
	}

	next{|time,callback|
		task = TaskProxy({|ev|
			var res_in,res_a,res_b,counter=0,step=0.01;
			var probIN=0,probA=0,probB=0,tmp;
			var tmpIN,tmpA,tmpB;
			var tmp_cue;

			res_in = List();
			res_a = List();
			res_b = List();

			res_in.clear();
			res_a.clear();
			res_b.clear();

			Ndef((\anal_++id).asSymbol).play;
			"analysing".postln;
			while{counter <= (ev.time)}{
				//".".post;
				bus_in.getn(melSize,{ arg val; tmpIN = val });
				bus_a.getn(melSize,{ arg val; tmpA = val });
				bus_b.getn(melSize,{ arg val; tmpB = val });

				if ((tmpIN != nil)&&(tmpA != nil)&&(tmpB != nil)){

					tmp = this.compareBuffer(tmpIN,tmpA,tmpB);

					if(tmp != nil){

						res_a.add(tmp[0].sum);
						res_b.add(tmp[1].sum);

						tmp = nil;

						counter = counter + step;

						if(counter >= (ev.time)){
							Ndef((\anal_++id).asSymbol).stop();
							Ndef((\anal_++id).asSymbol).free;// analyzer fix
//							Ndef(\anal).clear;// analyzer fix
							"analysis ended".postln;
							if(res_a.sum < res_b.sum){
								result = bufA;
								tmp_cue = bufB;
								allocator.set_state(bufA,2);
								allocator.set_state(bufB,1);
							}{
								result = bufB;
								tmp_cue = bufA;
								allocator.set_state(bufB,2);
								allocator.set_state(bufA,1);
							};
							postf("DECK: % , A: %, B: %, diff: %, result: %\n",id,bufA,bufB,abs(res_a.sum-res_b.sum),result);
							"progress: %".format(allocator.cue.size).postln;

							allocator.add_cue(tmp_cue);
							callback.value(result);

						};
					};
				};

				step.wait;
			};

		}).quant_(0);

		task.set(\time,time);
		^task.play;
	}


	compareBuffer{|in,a,b|
		var resA = List();
		var resB = List();
		in.do{|e,i|
			resA.add( abs(e - a[i]) );
			resB.add( abs(e - b[i]) );
	    };
		^[resA,resB];
	}



	freeBuffer{|buf,id,time|
		if(allocator.dynamic_allocation == true){
			allocator.set_state(buf.asSymbol,3);// set played
			{
				allocator.findandFreeBuffer(buf,id);
				time.wait;
			}.fork(AppClock);
		}{
			allocator.set_state(buf.asSymbol,3);// set played
		};
	}


    run{|time,callback|
	    var res = allocator.next;
		bufA = res[0];
		bufB = res[1];

		// redefine each time
		Ndef((\anal_++id).asSymbol,{|in,bufA,bufB,out1,out2,out3|
			var sig_in,sig_a,sig_b,
			fft_in,fft_a,fft_b,
			res_in,res_a,res_b;

			sig_in = In.ar(in);
			sig_a = PlayBuf.ar(2,bufA,BufRateScale.kr(bufA),1,0,0);
			sig_b = PlayBuf.ar(2,bufB,BufRateScale.kr(bufB),1,0,0);

			fft_in = FFT(buf_in,sig_in);
			fft_a = FFT(buf_a,sig_a);
			fft_b = FFT(buf_b,sig_b);

			res_in = MFCC.kr(fft_in, melSize);
			res_a = MFCC.kr(fft_a, melSize);
			res_b = MFCC.kr(fft_b, melSize);

			Out.kr(out1,res_in);
			Out.kr(out2,res_a);
			Out.kr(out3,res_b);

		}).quant_(0);

	    Ndef((\anal_++id).asSymbol).set(
		\in,0,
		\bufA,bufA,
		\bufB,bufB,
		\out1,bus_in,
		\out2,bus_a,
		\out3,bus_b);

		^this.next(time,callback);

     }

}