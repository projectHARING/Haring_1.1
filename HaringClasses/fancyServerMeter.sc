////////////////////////////////////////////////////////////////////////////////////////////////////
//
// FancyServerMeter
//
// * Minimal fancy looks for Server's meter via hacking the original ServerMeterView
//
// by Darien Brito
// http://www.darienbrito.com
//
//

ServerMeterView2 : ServerMeterView {

	init { arg aserver, parent, leftUp, anumIns, anumOuts;
		var innerView, viewWidth, levelIndicL,levelIndicR, palette;
		var totalWidth = 280;

		server = aserver;

		numIns = anumIns ?? { server.options.numInputBusChannels };
		numOuts = anumOuts ?? { server.options.numOutputBusChannels };

		viewWidth= this.class.getWidth(anumIns, anumOuts);

		leftUp = leftUp ? (0@0);

		view = CompositeView(parent, Rect(leftUp.x, leftUp.y, totalWidth, height) );
		view.onClose_( { this.stop });
		innerView = CompositeView(view, Rect(10, 0, totalWidth, height) ); // Change margin
		innerView.addFlowLayout(0@0, gapWidth@gapWidth);

/*		// dB scale
		UserView(innerView, Rect(0, 0, meterWidth, 195)).drawFunc_( {
			try {
				Pen.color = \QPalette.asClass.new.windowText;
			} {
				Pen.color = Color.white;
			};
			Pen.font = Font.sansSerif(10).boldVariant;
			Pen.stringCenteredIn("0", Rect(0, 0, meterWidth, 12));
			Pen.stringCenteredIn("-80", Rect(0, 170, meterWidth, 12));
		});

		if(numIns > 0) {
			// ins
			StaticText(view, Rect(10, 5, 100, 15))
			.font_(Font.sansSerif(10).boldVariant)
			.string_("Inputs");
			inmeters = Array.fill( numIns, { arg i;
				var comp;
				comp = CompositeView(innerView, Rect(0, 0, meterWidth, 195)).resize_(5);
				StaticText(comp, Rect(0, 180, meterWidth, 15))
				.font_(Font.sansSerif(9).boldVariant)
				.string_(i.asString);
				levelIndic = LevelIndicator( comp, Rect(0, 0, meterWidth, 180) ).warning_(0.9).critical_(1.0)
				.drawsPeak_(true)
				.numTicks_(9)
				.numMajorTicks_(3);
			});
		};

		if((numIns > 0) && (numOuts > 0)) {
			// divider
			UserView(innerView, Rect(0, 0, meterWidth, 180)).drawFunc_( {
				try {
					Pen.color = \QPalette.asClass.new.windowText;
				} {
					Pen.color = Color.white;
				};
				Pen.line(((meterWidth + gapWidth) * 0.5)@0, ((meterWidth + gapWidth) * 0.5)@180);
				Pen.stroke;
			});
		};*/

		// outs
		if(numOuts > 0) {

	/*		StaticText(view, Rect(10 + if(numIns > 0) { (numIns + 2) * (meterWidth + gapWidth) } { 0 }, 5, 100, 15))
			.stringColor_(Color.white)
			.font_(Font("Helvetica", 9, true))
			.string_("Outputs");*/


			/*outmeters = Array.fill( numOuts, { arg i;
				var comp;
				var labels = [\MasterL, \MasterR, \Deck1L, \Deck1R, \Deck2L, \Deck2R, \Deck3L, \Deck3R, \Deck4L, \Deck4R];
				comp = CompositeView(innerView, Rect(0, 0, totalWidth, 10));

				levelIndic = LevelIndicator( comp, Rect(0, 0, 220, 10) ).warning_(0.9).critical_(1.0)
				.meterColor_(Color.green(1, 0.75))
				.drawsPeak_(false)
				.numTicks_(0)
				.numMajorTicks_(0);

				StaticText(comp, Rect(230, -1, 60, 15))
				.stringColor_(Color.white)
				.font_(Font("Helvetica", 9, true))
				.string_(labels[i]);

				levelIndic;
			});*/

			var labels = [\Master, \Deck1, \Deck2, \Deck3,\Deck4];
			outmeters = List();


			labels.do{|obj,i|
				var comp;

				comp = CompositeView(innerView, Rect(0, 0, totalWidth, 10));

				levelIndicL= LevelIndicator( comp, Rect(60, 0, 220, 5) ).warning_(0.9).critical_(1.0)
				.meterColor_(Color.green(1, 0.75))
				.drawsPeak_(false)
				.numTicks_(0)
				.numMajorTicks_(0);

				outmeters.add(levelIndicL);

				levelIndicR= LevelIndicator( comp, Rect(60, 5, 220, 5) ).warning_(0.9).critical_(1.0)
				.meterColor_(Color.green(1, 0.75))
				.drawsPeak_(false)
				.numTicks_(0)
				.numMajorTicks_(0);

				outmeters.add(levelIndicR);

				StaticText(comp, Rect(0, 0, 60, 10))
				.stringColor_(Color.white)
				.font_(Font("Helvetica", 9,false))
				.string_(labels[i]);

			};




		};

		this.setSynthFunc(inmeters, outmeters);
		startResponderFunc = {this.startResponders};
		this.start;
	}

}

FancyServerMeter {

	var <window, <meterView, <styler;

	*new { |server, numIns, numOuts, masterWin, posX = 0, posY = 0|

		var window, meterView, styler;

		numIns = numIns ?? { server.options.numInputBusChannels };
		numOuts = numOuts ?? { server.options.numOutputBusChannels };
		styler = GUIStyler(masterWin);

		window = styler.getWindow(server.name ++ " levels (dBFS)",
			Rect(posX, posY, ServerMeterView2.getWidth(numIns, numOuts) + 100, ServerMeterView2.height),
			false);

		meterView = ServerMeterView2(server, window, 0@0, numIns, numOuts);
		meterView.view.keyDownAction_( { arg view, char, modifiers;
			if(modifiers & 16515072 == 0) {
				case
				 {char === 27.asAscii } { window.close };
			};
		});

		window.front;

		^super.newCopyArgs(window, meterView, styler)

	}

}