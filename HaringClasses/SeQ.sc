////////////////////////////////////////////////////////////////////////////////////////////////////
//
// seQ
//
// * tracks sequence
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


SeQ {
	var <x,<y,<size;
	var <win,view,cells,height,run;

	*new{|x,y,size|
		^super.newCopyArgs(x,y,size).init;
	}

	init{
		run = true;
		height = 46;
		this.init_state;
		//this.init_view;
	}

	init_state{
		cells = Array.fill(size,{-1});
	}

	init_view { | masterWindow |
		var color;
		var width = 94*5.79; // static width size
		run = true;
		view = UserView(masterWindow, Rect(x,y,width,height));
		view.clearOnRefresh_(false);
		view.background_(Color.black);
		view.drawFunc = {
			size.do{|x|
				color = case
				{cells[x]== -1}{color = Color.gray(0.2,1)}// nil
				{cells[x]== 0}{color = Color.blue}// loading
				{cells[x]== 1}{color = Color.gray(0.4, 1)}// loaded
				{cells[x]== 2}{color = Color.green(1, 0.75)}// playing
				{cells[x]== 3}{color = Color.gray(0.2,1)}// played
				{cells[x]== 4}{color = Color.new255(192,191,224)};// analising
				Pen.fillColor = color;
				Pen.fillRect(Rect(x*(width/size),0,(width/size),height));
				Pen.fill;
			};
		};
		view.refresh;
		this.draw;
	}

	draw{
		{ while { run } {
			view.refresh;
			0.1.wait;
		}
		}.fork(AppClock);
	}

	update{|i,state|
		cells[i] = state;
	}

	reset{
		cells.size.do{|i|
			cells[i] = 1;
		}
	}

	close{
		view.close;
	}

}