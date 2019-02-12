var app = require("express")();
var server = require("http").Server(app);
var io = require("socket.io")(server);
var port = 3000;

var users = [];

server.listen(port, function(){
	console.log("Server running at " + port);
});

io.on("connection", function(socket){
	
	socket.on("disconnect", function(){
		
		for(var i = 0; i < users.length; i++){
			if(users[i].id == socket.id){
				socket.broadcast.emit("user_disconnected", users[i].name);
				console.log(users[i].name + " has disconnected");
				users.splice(i, 1);
			}
		}
	});
	
	socket.on("set_name", function(user){
			
			console.log("User connected : " + user);
			
			socket.broadcast.emit("new_user", user);
			
			socket.emit("get_users", users);
			
			users.push(new User(socket.id, user));
	});
	
	socket.on("user_message", function(data){
		socket.broadcast.emit("user_message", data);
		socket.emit("user_message", data);
	});
	
});

function User(id, name){
	this.id = id;
	this.name = name;
}

