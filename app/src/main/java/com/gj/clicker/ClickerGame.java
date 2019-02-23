package com.gj.clicker;

import java.util.ArrayList;
import java.util.HashMap;

import com.ovl.engine.OverloadEngine;
import com.ovl.engine.ParamSetter;
import com.ovl.engine.ParamSetterFactory;
import com.ovl.engine.Renderer;
import com.ovl.engine.Renderer.PrimitiveType;
import com.ovl.engine.Shader;
import com.ovl.engine.Vbo;
import com.ovl.engine.android.MutableFloat;
import com.ovl.game.BaseGame;
import com.ovl.game.GameObject;
import com.ovl.graphics.Color;
import com.ovl.graphics.CustomFont;
import com.ovl.graphics.Primitive;
import com.ovl.graphics.SimpleFont;
import com.ovl.graphics.Sprite;
import com.ovl.physics.PhysicsBody.BodyType;
import com.ovl.ui.Button;
import com.ovl.ui.Label;
import com.ovl.ui.OnClickListener;
import com.ovl.utils.FastMath;
import com.ovl.utils.OverloadRandom;
import com.ovl.utils.Paths;
import com.ovl.utils.Vector2;

public class ClickerGame extends BaseGame {
	static final int STATE_MENU = 0;
	static final int STATE_GAME = 1;
	static final int STATE_END = 2;
	static final int STATE_ROUND_END = 3;
	
	static final int MAXPLAYERS = 8;
	static final int MINPLAYERS = 2;
	static final int MAXROUNDS = 5;
	static final int MINROUNDS = 1;
	static final int MAXTIME = 8;
	static final int MINTIME = 1;
	
	int state = STATE_MENU;
	int numPlayers = 2;
	
	CustomFont gameFont;
	
	// Ingame stuff
	Label valueLabel;
	GameObject valueField;
	GameObject valueFieldBg;
	SimpleFont valueLabels[] = new SimpleFont[MAXPLAYERS];
	GameObject playerField[] = new GameObject[MAXPLAYERS];
	Vector2 fieldVerts[][] = new Vector2[MAXPLAYERS][];
	int values[] = new int[MAXPLAYERS];	
	int totals[] = new int[MAXPLAYERS];
	boolean clicked[] = new boolean[MAXPLAYERS];
	
	// Menu stuff
	GameObject menuBg;
	Button play;
	SimpleFont playersText;
	SimpleFont playText;
	SimpleFont roundOption;
	SimpleFont timeOption;
	Button playersUp;
	Button playersDown;
	Button roundsUp;
	Button roundsDown;
	Button timeUp;
	Button timeDown;
	
	// End stuff
	GameObject crownObj[];
	
	Renderer renderer;
	ArrayList<Vector2> clickQueue = new ArrayList<Vector2>();
	
	float gameDuration = 5.0f;
	int numRounds = 1;
	int round = 0;
	
	final int maxVal = 1000;
	final int startVal = 500;
	int value = startVal;
	final float valueUpdateInterval = 0.05f;
	float valueUpdateTimer = 0.0f;
	float directionChangeTimer = 0.0f;
	float directionChangeInterval = 0.05f;
	int direction = 1;
	
	MutableFloat fadeTimer = new MutableFloat(0.0f);
	float stateTimer = 0.0f;
	
	public void reset(){
		value = startVal;
		valueUpdateTimer = 0.0f;
		directionChangeTimer = 0.0f;
		directionChangeInterval = 0.05f;
		direction = 1;
		
		for (int i = 0; i < MAXPLAYERS; ++i){
			clicked[i] = false;
			values[i] = 0;
			valueLabels[i].setText("0");
		}
	}
	
	public void fillCircle(Vector2[] verts, float sa, float radius){
		float cry = radius * OverloadEngine.getInstance().aspectRatio;
		float da = (360.0f - sa) / (verts.length - 2);	
		float a = 360.0f;
		float d2r = FastMath.DEG2RAD;
		for (int i = verts.length - 1; i > 0; a -= da, --i){
			verts[i].set((float)Math.cos(a * d2r), (float)Math.sin(a * d2r)).mul(radius, cry);
		}
	}
	
	public final int clamp(int v, int min, int max){
		return Math.max(min, Math.min(v, max));
	}
	
	@Override
	public void init() {
		super.init();
		
		renderer = OverloadEngine.getInstance().renderer;
		gameFont = renderer.getFontBuilder().buildFont("res/gamefont.ttf").deriveFont(72.0f);
		
		/**
		 *  ===================== MENU ==========================
		 */

		CustomFont smallerFont = gameFont.deriveFont(44.0f);
		SimpleFont text2plr = SimpleFont.create("2 players", smallerFont);
		text2plr.setPosition(-0.65f, 0.25f);

		menuBg = new GameObject(this);
		menuBg.initEntity(BodyType.NON_INTERACTIVE);
		menuBg.setSprite(new Primitive(new Vector2[]{
			new Vector2(-1.0f, -1.0f),
			new Vector2(-1.0f, 1.0f),
			new Vector2(1.0f, 1.0f),
			new Vector2(1.0f, -1.0f),
		}, Renderer.PrimitiveType.TriangleFan));
		menuBg.setColor(new Color(0.7f, 0.7f, 0.7f, 1.0f));
		addObject(menuBg);
		
		play = new Button(this, "");
		play.setPosition(0.0f, -0.52f);
		play.setScale(0.75f, 0.75f);
		play.setClickListener(new OnClickListener() {		
			@Override
			public void clickFunction(Vector2 pos) {
				startGame();
				changeState(STATE_GAME);
			}
		});
		addObject(play);
		
		playText = SimpleFont.create("Go", smallerFont);
		playText.setPosition(0.0f, -0.52f);
		addObject(playText);
		
		playersText = SimpleFont.create("2 players", smallerFont);
		playersText.setPosition(0.0f, 0.7f);
		addObject(playersText);
		
		playersUp = new Button();
		playersUp.setPosition(playersText.getPosition().copy().add(0.35f, 0.0f));
		playersUp.setSprite(new Sprite(Paths.getResources() + "plus.png"));
		playersUp.setClickListener(new OnClickListener(){

			@Override
			public void clickFunction(Vector2 pos) {
				numPlayers = clamp(numPlayers + 1, MINPLAYERS, MAXPLAYERS);
				playersText.setText(String.format("%d players", numPlayers));
			}
			
		});
		addObject(playersUp);
		
		playersDown = new Button();
		playersDown.setPosition(playersText.getPosition().copy().sub(0.35f, 0.0f));
		playersDown.setSprite(new Sprite(Paths.getResources() + "minus.png"));
		playersDown.setClickListener(new OnClickListener(){

			@Override
			public void clickFunction(Vector2 pos) {
				numPlayers = clamp(numPlayers - 1, MINPLAYERS, MAXPLAYERS);
				playersText.setText(String.format("%d players", numPlayers));
			}
			
		});
		addObject(playersDown);
		
		timeOption = SimpleFont.create("5 seconds", smallerFont);
		timeOption.setPosition(0.0f, 0.45f);
		addObject(timeOption);
		
		timeUp = new Button();
		timeUp.setPosition(timeOption.getPosition().copy().add(0.35f, 0.0f));
		timeUp.setSprite(new Sprite(Paths.getResources() + "plus.png"));
		timeUp.setClickListener(new OnClickListener(){

			@Override
			public void clickFunction(Vector2 pos) {
				gameDuration = clamp((int)(gameDuration + 1.0f), MINTIME, MAXTIME);
				timeOption.setText(String.format("%d second%s", (int)gameDuration, (int)gameDuration == 1 ? "" : "s"));
			}
			
		});
		addObject(timeUp);
		
		timeDown = new Button();
		timeDown.setPosition(timeOption.getPosition().copy().sub(0.35f, 0.0f));
		timeDown.setSprite(new Sprite(Paths.getResources() + "minus.png"));
		timeDown.setClickListener(new OnClickListener(){

			@Override
			public void clickFunction(Vector2 pos) {
				gameDuration = clamp((int)(gameDuration - 1.0f), MINTIME, MAXTIME);
				timeOption.setText(String.format("%d second%s", (int)gameDuration, (int)gameDuration == 1 ? "" : "s"));
			}
			
		});
		addObject(timeDown);
		
		roundOption = SimpleFont.create("1 round", smallerFont);
		roundOption.setPosition(0.0f, 0.2f);
		addObject(roundOption);
		
		roundsUp = new Button();
		roundsUp.setPosition(roundOption.getPosition().copy().add(0.35f, 0.0f));
		roundsUp.setSprite(new Sprite(Paths.getResources() + "plus.png"));
		roundsUp.setClickListener(new OnClickListener(){

			@Override
			public void clickFunction(Vector2 pos) {
				numRounds = clamp(numRounds + 1, MINROUNDS, MAXROUNDS);
				roundOption.setText(String.format("%d round%s", numRounds, numRounds == 1 ? "" : "s"));
			}
			
		});
		addObject(roundsUp);
		
		roundsDown = new Button();
		roundsDown.setPosition(roundOption.getPosition().copy().sub(0.35f, 0.0f));
		roundsDown.setSprite(new Sprite(Paths.getResources() + "minus.png"));
		roundsDown.setClickListener(new OnClickListener(){

			@Override
			public void clickFunction(Vector2 pos) {
				numRounds = clamp(numRounds - 1, MINROUNDS, MAXROUNDS);
				roundOption.setText(String.format("%d round%s", numRounds, numRounds == 1 ? "" : "s"));
			}
			
		});
		addObject(roundsDown);
		
		/**
		 *  ===================== GAME ==========================
		 */		
		Color playerColors[] = {
			new Color(0.8f, 0.1f, 0.1f),
			new Color(0.1f, 0.1f, 0.8f),
			new Color(0.1f, 0.8f, 0.1f),
			new Color(0.9f, 0.9f, 0.0f),
			new Color(0.6f, 0.2f, 0.8f),
			new Color(0.2f, 0.9f, 0.5f),
			new Color(0.7f, 0.7f, 0.7f),
			new Color(0.1f, 0.1f, 0.1f),
		};
		
		Renderer r = OverloadEngine.getInstance().renderer;
		Shader distShader = r.createShader("Dist");
		
		ParamSetter ff = ParamSetterFactory.build(distShader, "u_Time", fadeTimer);
		
		for (int i = 0; i < MAXPLAYERS; ++i){
			fieldVerts[i] = new Vector2[]{new Vector2(), new Vector2(), new Vector2(), new Vector2(), new Vector2()};
			Primitive p = new Primitive(fieldVerts[i], PrimitiveType.Polygon);
			Vbo distVbo = r.createVbo("Dist", fieldVerts[i].length, fieldVerts[i].length);
			HashMap<String, ParamSetter> shaderParams = new HashMap<String, ParamSetter>();
			shaderParams.put("u_Time", ff);
			shaderParams.put(Shader.U_COLOR, ParamSetterFactory.build(distShader, Shader.U_COLOR, p.getColor()));
			shaderParams.put(Shader.U_MVPMATRIX, ParamSetterFactory.buildDefault(distShader, Shader.U_MVPMATRIX));
			
			p.setColor(playerColors[i]);
			p.useShader(distVbo, shaderParams);
			
			playerField[i] = new GameObject(this);
			playerField[i].initEntity(BodyType.NON_INTERACTIVE);
			playerField[i].setSprite(p);
			addObject(playerField[i]);
		}
			
		for (int i = 0; i < MAXPLAYERS; ++i){
			valueLabels[i] = SimpleFont.create("---");
			valueLabels[i].setFont(gameFont);
			valueLabels[i].setVisible(false);
			addObject(valueLabels[i]);
		}
		
		final Vector2 cbgv[] = new Vector2[46];
		for (int i = 0; i < cbgv.length; ++i){
			cbgv[i] = new Vector2();
		}
		fillCircle(cbgv, 0.0f, 0.25f);
		Primitive circlebg = new Primitive(cbgv, Renderer.PrimitiveType.Polygon);
		
		valueFieldBg = new GameObject(this);
		valueFieldBg.initEntity(BodyType.NON_INTERACTIVE);
		valueFieldBg.setSprite(circlebg);
		valueFieldBg.setColor(new Color(0.5f, 0.5f, 0.5f, 0.5f));
		addObject(valueFieldBg);
		
		final Vector2 cv[] = new Vector2[33];
		for (int i = 0; i < cv.length; ++i){
			cv[i] = new Vector2();
		}
		fillCircle(cv, 0.0f, 0.26f);
		Primitive circle = new Primitive(cv, Renderer.PrimitiveType.Polygon);
		valueField = new GameObject(this){
			float t = 0.0f;
			float angle = 0.0f;
			
			public void update(float deltaTime) {
				if (state != STATE_GAME){
					return;
				}
				
				t += deltaTime;
				angle = (359.9f / gameDuration) * t;
				
				if (t > gameDuration){
					t = 0.0f;
					angle = 0.0f;
					
					for (int i = 0; i < MAXPLAYERS; ++i){
						totals[i] += values[i];
					}

					round++;
					if (round < numRounds){
						changeState(STATE_ROUND_END);
						setVisibleObjs(STATE_GAME, true);
						reset();
					}
					else
					{
						changeState(STATE_END);
						setVisibleObjs(STATE_GAME, true);
						gameEnd();
					}
				}				

				Primitive p = (Primitive)getSprite();
				fillCircle(cv, angle, 0.26f);
				p.refreshVertexData();
			};
		};
		valueField.initEntity(BodyType.NON_INTERACTIVE);
		valueField.setSprite(circle);
		valueField.setColor(new Color(0.0f, 0.0f, 0.0f));
		addObject(valueField);
		
		valueLabel = new Label(this, "000"){
			public void update(float deltaTime) {
				if (state != STATE_GAME){
					return;
				}

				fadeTimer.f += deltaTime;
				valueUpdateTimer += deltaTime;
				directionChangeTimer += deltaTime;
				value += direction;
				
				direction = direction > 0 ? direction + 1 : direction - 1;
				
				if (value <=  0 || value >= maxVal){
					direction = direction > 0 ? -1 : 1;
					directionChangeTimer = 0.0f;
					value = clamp(value, 0, maxVal);
				}
				
				if (directionChangeTimer >= directionChangeInterval){
					directionChangeTimer -= directionChangeInterval;
					directionChangeInterval = (float)OverloadRandom.next(25) * 0.01f + 0.05f;
					int oldDir = direction;
					direction = OverloadRandom.next(2) == 0 ? -1 : 1;
					if (Math.signum(direction) == Math.signum(oldDir)){
						direction = oldDir;
					}
				}
				
				if (valueUpdateTimer >= valueUpdateInterval){
					valueUpdateTimer -= valueUpdateInterval;
					setText("" + value);
				}
			}
		};
		valueLabel.setFont(gameFont.deriveFont(128.0f));
		addObject(valueLabel);
		
		// ============= END ================
		
		crownObj = new GameObject[MAXPLAYERS];
		for (int i = 0; i < MAXPLAYERS; ++i){
			crownObj[i] = new GameObject(this);
			crownObj[i].initEntity(BodyType.NON_INTERACTIVE);
			crownObj[i].setSprite(new Sprite(Paths.getResources() + "crown.png"));
			//crownObj.setScale(0.5f, 0.5f);
			crownObj[i].setVisible(false);
			addObject(crownObj[i]);
		}
		
		// ==================================
		
		reset();
		setVisibleObjs(STATE_MENU, false);
		setVisibleObjs(STATE_GAME, false);
		setVisibleObjs(STATE_END, false);
		changeState(STATE_MENU);
	}
	
	public void changeState(int s){
		setVisibleObjs(state, false);
		setVisibleObjs(s, true);
		state = s;
		stateTimer = 0.0f;
	}
	
	public void setVisibleObjs(int s, boolean show){
		switch (s){
			case STATE_MENU:{
				menuBg.setVisible(show);
				playersText.setVisible(show);
				play.setVisible(show);
				playText.setVisible(show);
				roundOption.setVisible(show);
				timeOption.setVisible(show);
				playersUp.setVisible(show);
				playersDown.setVisible(show);
				timeUp.setVisible(show);
				timeDown.setVisible(show);
				roundsUp.setVisible(show);
				roundsDown.setVisible(show);
				break;
			}
			case STATE_GAME:{
				valueLabel.setVisible(show);
				valueFieldBg.setVisible(show);
				valueField.setVisible(show);
				for (int i = 0; i < MAXPLAYERS; ++i){
					playerField[i].setVisible(show && i < numPlayers);
					valueLabels[i].setVisible(show && i < numPlayers);
				}
				break;
			}
			case STATE_END:{
				for (int i = 0; i < MAXPLAYERS; ++i){
					crownObj[i].setVisible(show && i < numPlayers);
				}
				break;
			}
		}
	}
	
	public void startGame(){
		float angle = 90.0f;
		float da = 360.0f / numPlayers;
		float radius = 2.5f;

		float d2r = FastMath.DEG2RAD;
		float cry = radius/* * OverloadEngine.getInstance().aspectRatio*/;
		Vector2 verts[] = new Vector2[numPlayers];
		for (int i = 0; i < numPlayers; ++i){
			verts[i] = new Vector2((float)Math.cos(angle * d2r), (float)Math.sin(angle * d2r)).mul(radius, cry);
			angle += da;
		}
		
		int n = numPlayers - 1;
		angle -= da * 0.5f;
		for (int i = 0; i < numPlayers; ++i){
			Primitive p = (Primitive)playerField[i].getSprite();
			fieldVerts[i][1] = verts[n].copy();
			n = (n + 1) % numPlayers;
			float la = angle - 10.0f;
			float ra = angle + 10.0f;
			fieldVerts[i][2] = new Vector2((float)Math.cos(la * d2r), (float)Math.sin(la * d2r)).mul(radius, cry);
			fieldVerts[i][3] = new Vector2((float)Math.cos(ra * d2r), (float)Math.sin(ra * d2r)).mul(radius, cry);
			fieldVerts[i][4] = verts[n].copy();
			
			Vector2 labelPos = new Vector2((float)Math.cos(angle * d2r), (float)Math.sin(angle * d2r)).mul(radius, cry);
			valueLabels[i].setPosition(labelPos.normalize().mul(0.8f));
			valueLabels[i].setRotation(angle + 90.0f);
			//n = (n + 1) % numPlayers;
			p.refreshVertexData();
			angle += da;
		}
		
		fadeTimer.f += 0.0f;
	}
	
	public void gameEnd(){
		int highValue = 0;
		int index = -1;
		ArrayList<Integer> winners = new ArrayList<Integer>();
		for (int i = 0; i < MAXPLAYERS; ++i){
			if (totals[i] > highValue){
				highValue = totals[i];
				index = i;
				winners.clear();
				winners.add(i);
			}
			else if (totals[i] == highValue && highValue != 0)
			{
				winners.add(i);
			}
			
			crownObj[i].setVisible(false);
		}
		
		if (index == -1){
			crownObj[0].setRotation(0.0f);
			crownObj[0].setPosition(0.0f, 0.0f);
			crownObj[0].setVisible(true);
		}
		else
		{
			for (Integer i : winners){
				Vector2 cp = valueLabels[i].getPosition().copy().add(0.0f, 0.3f);
				crownObj[i].setPosition(cp.rotateAroundPoint(valueLabels[i].getPosition(), valueLabels[i].getRotation()));
				crownObj[i].setRotation(valueLabels[i].getRotation());
				crownObj[i].setScale(0.5f, 0.5f);
				crownObj[i].setVisible(true);
			}
		}
		
		round = 0;
		for (int i = 0; i < MAXPLAYERS; ++i){
			valueLabels[i].setText("" + totals[i]);
			totals[i] = 0;
		}
	}
	
	@Override
	public boolean onClick(Vector2 pos) {
		switch (state){
			case STATE_GAME:{
				float da = 360.0f / numPlayers;
				float angleOffset = 90.0f + (numPlayers - 1) * da;
				float clickAngle = (pos.angle() * FastMath.RAD2DEG + angleOffset) / da;
				clickAngle += 1.0f - (0.5f * (numPlayers - 1));
				int player = Math.round(clickAngle) % numPlayers;
				if (player < 0){
					player = numPlayers - 1;
				}
				
				if (!clicked[player])
				{
					valueLabels[player].setText("" + value);
					values[player] = value;
					clicked[player] = true;
				}
				break;
			}
			case STATE_MENU:{
				play.onClick(pos);
				playersUp.onClick(pos);
				playersDown.onClick(pos);
				timeUp.onClick(pos);
				timeDown.onClick(pos);
				roundsUp.onClick(pos);
				roundsDown.onClick(pos);
				break;
			}
			case STATE_END:{
				reset();
				setVisibleObjs(STATE_GAME, false);
				changeState(STATE_MENU);
				break;
			}
			case STATE_ROUND_END:{
				changeState(STATE_GAME);
				break;
			}
		}
		
		return false;
	}

	public void postClick(Vector2 pos){
		synchronized (clickQueue){
			clickQueue.add(pos);
		}
	}
	
	@Override
	public void update(float deltaTime) {
		synchronized (clickQueue){
			if (stateTimer > 0.1f){
				for (Vector2 i : clickQueue){
					onClick(i);
				}
			}
			clickQueue.clear();
		}
		
		stateTimer += deltaTime;
		
		super.update(deltaTime);
	}
}
