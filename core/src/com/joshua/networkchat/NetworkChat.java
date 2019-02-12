package com.joshua.networkchat;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.List;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextArea;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import com.badlogic.gdx.scenes.scene2d.ui.Window;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonWriter;
import com.badlogic.gdx.utils.viewport.ScreenViewport;

import org.json.JSONArray;
import org.json.JSONException;

import java.net.URISyntaxException;

import io.socket.client.IO;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;


public class NetworkChat extends ApplicationAdapter {

	private Stage stage;
	private Skin skin;

	private Table loginTable, chatRoomTable;

	private Socket socket;

	private String username;

	@Override
	public void create () {
		skin = new Skin(Gdx.files.internal("uiskin.json"));
		stage = new Stage(new ScreenViewport());
		Gdx.input.setInputProcessor(stage);

		loginTable = buildLoginTable();
		chatRoomTable = buildChatRoomTable();

		stage.addActor(loginTable);
		stage.addActor(chatRoomTable);
	}

	// login table actors

	private TextButton join_button;
	private TextField name_field;

	private Table buildLoginTable(){
		final Table table = new Table();
		table.setFillParent(true);

		Window window = new Window("Login", skin);
		window.getTitleLabel().setAlignment(Align.center);

		join_button = new TextButton("Join", skin);
		name_field = new TextField("", skin);

		window.add(new Label("Enter Your Name", skin));
		window.row();
		window.add(name_field);
		window.row();
		window.add(join_button);

		table.add(window);

		join_button.addListener(new ChangeListener() {
			@Override
			public void changed(ChangeEvent event, Actor actor) {

                username = name_field.getText();

				if(!username.isEmpty()){
					loginTable.setVisible(false);
					chatRoomTable.setVisible(true);

					try {
						socket = IO.socket("http://localhost:3000");
						socket.connect();
						handleSocketEvents();
					} catch (URISyntaxException e) {
						e.printStackTrace();
					}



				}
			}
		});



		return table;
	}

	// chat room table actors

	private TextButton send_button;
	private TextArea message_field;
	private ScrollPane input_scroll;

	private List<String> users_list;
	private ScrollPane chat_scroll;
	private ScrollPane users_scroll;
	private Label chat_label;


	private Table buildChatRoomTable(){
		Table table = new Table();
		table.setFillParent(true);

		chat_label = new Label("", skin);
		chat_label.setWrap(true);
		chat_label.setAlignment(Align.topLeft);

		chat_scroll = new ScrollPane(chat_label, skin);
		chat_scroll.setFadeScrollBars(false);

		table.add(chat_scroll).width(300f).height(400f).colspan(2);

		users_list = new List<String>(skin, "dimmed");

		users_scroll = new ScrollPane(users_list, skin);
		users_scroll.setFadeScrollBars(false);

		table.add(users_scroll).width(150f).height(400f).colspan(2);

		message_field = new TextArea("", skin);
		message_field.setPrefRows(2);

		input_scroll = new ScrollPane(message_field, skin);
		input_scroll.setFadeScrollBars(false);

		table.row();
		table.add(input_scroll).width(300f).colspan(2);

		send_button = new TextButton("Send", skin);
		table.add(send_button).colspan(2);

		table.setVisible(false);

		send_button.addListener(new ChangeListener() {
			@Override
			public void changed(ChangeEvent event, Actor actor) {

				String text = message_field.getText();

				if(!text.isEmpty()){
					socket.emit("user_message", username + ": " + text + "\n");
					message_field.setText("");
				}

			}
		});

		return table;
	}

	private void handleSocketEvents(){

		final Json json = new Json();
		json.setOutputType(JsonWriter.OutputType.json);

		socket.on(Socket.EVENT_CONNECT, new Emitter.Listener() {
			@Override
			public void call(Object... args) {

				socket.emit("set_name", username);

			}
		}).on("get_users", new Emitter.Listener() {
			@Override
			public void call(Object... args) {

				JSONArray array = (JSONArray)args[0];

				Array<String> users = new Array<String>();

				try{
					for(int i = 0; i < array.length(); i++){
						String name = array.getJSONObject(i).getString("name");
						users.add(name);
					}
				}catch (JSONException e){

				}
				users.add(username);
				users_list.setItems(users);
			}
		}).on("user_message", new Emitter.Listener() {
			@Override
			public void call(Object... args) {

				String text = (String)args[0];

				chat_label.setText(chat_label.getText() + text);


			}
		}).on("new_user", new Emitter.Listener() {
			@Override
			public void call(Object... args) {

				String name = (String)args[0];

				Array<String> users = users_list.getItems();
				users.add(name);
				users_list.setItems(users);

			}
		}).on("user_disconnected", new Emitter.Listener() {
			@Override
			public void call(Object... args) {

				String name = (String)args[0];

				Array<String> users = users_list.getItems();

				if(users.contains(name, false)){
					users.removeValue(name, false);
					users_list.setItems(users);
				}

			}
		});

	}

	@Override
	public void render () {
		Gdx.gl.glClearColor(0.2f, 0.2f, 0.2f, 1);
		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

		stage.act(Gdx.graphics.getDeltaTime());
		stage.draw();
	}

	@Override
	public void resize(int width, int height){
		stage.getViewport().update(width, height, true);
	}

	@Override
	public void dispose () {
		stage.dispose();
		skin.dispose();
	}
}
