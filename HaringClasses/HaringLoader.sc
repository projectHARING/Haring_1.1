////////////////////////////////////////////////////////////////////////////////////////////////////
//
// HaringLoader
//
// * Initialization
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

HaringLoader {
	classvar server;
	classvar tPath;
	classvar divisions;
	classvar dynamic;

	*new { | path |
		server = Server.default;
		dynamic = true; // set default static allocation
		this.setOutputs;
		this.builInitGUI(path);
	}

	*setOutputs {
		server.options.numInputBusChannels = 0;
		server.options.numOutputBusChannels = 10;
	}

	*builInitGUI { |path|
		var width = 320;
		var master = Window("",Rect(0,800,width,150)).background_(Color.black);//.alpha_(0.875);
		var styler = GUIStyler(master);
		var win = styler.getWindow("Audio Settings", width@200, border: true);
		var decorator;
		var currentPath;
		var pop,check;
	
		win.decorator = FlowLayout(win.bounds);
		decorator = win.decorator;
		styler.getSubtitleText(win, "Welcome to the H.A.R.I.N.G", decorator);
		decorator.nextLine;
		styler.getSubtitleText(win, "Output device:", decorator,align:\right,width:80);
		styler.getPopUpMenu(win, 220)
		.items_(ServerOptions.outDevices)
		.action = {|item|
			server.options.outDevice = item;
		};
		decorator.nextLine;

		// //set default divisions
		divisions = "4";
		styler.getSubtitleText(win, "Division:",decorator,align:\right,width:80);
		pop = styler.getPopUpMenu(win, 220)
		 .items_(["2","3","4","5","6","7","8","9","10","none"])
		 .action = {|menu|
			var div;
			divisions = menu.item;
			if(divisions != "none"){
				div = divisions.asInteger * 2;
				divisions = (div/2);//to match PDC
			};
		 };

		pop.value = 2;

		decorator.nextLine;
		//
		styler.getSubtitleText(win, "Dynamic allocation:", decorator,align:\right,width:80);
		check = styler.getCheckBox(win,"")
		 //.value = dynamic
		 .action = {|check|
			dynamic = check.value;
		};

		check.value_(dynamic);

		

		decorator.nextLine;

		styler.getSubtitleText(win, "Tracks path:", decorator,align:\right,width:80);
		currentPath = styler.getMultiLineText(win, 196@15)
		.string_(path++"Tracks/")
		.stringColor_(Color.white);
		tPath = path ++ "Tracks/";
		styler.getSizableButton(win, "...",size: 20@15)
		.action = {|btn|
			FileDialog({ | localPath |
				localPath.postln;
				currentPath.string_(localPath.first);
				currentPath.stringColor_(Color.white);
				tPath = localPath.first ++ "/";
			},{
				"cancelled".postln;
			}, 2)
		};

		decorator.nextLine;

		styler.getSubtitleText(win, "", decorator,align:\right,width:250);

		styler.getButton(win, "Run")
		.action = {|btn|
			var warningMaster, warning, styler, decorator;
			warningMaster = Window("Warning", Rect(0,720,200,80));
			styler = GUIStyler(warningMaster);
			warning = styler.getWindow("Warning", 200@80).background_(Color.red).alpha_(0.85);
			warning.decorator = FlowLayout(warning.bounds);
			decorator = warning;
			styler.getMultiLineText(warning,190@40)
			.string_("Warning: this will create a copy of all the tracks in this folder, are you sure you have enough space?")
			.stringColor_(Color.white);
			styler.getButton(warning, "Yes")
			.action = { |btn|
				server.boot;
				server.doWhenBooted {
					master.close;
					warningMaster.close;
						Haring(path, 4, tPath, divisions.asString,dynamic);
				}
			};
			styler.getButton(warning, "No")
			.action = { |btn|
				warningMaster.close;
			};
			warningMaster.front;
		};
		master.front;
	}
}
