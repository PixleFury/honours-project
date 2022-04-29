const express = require("express");
const session = require("express-session");
const https = require("https");
const http = require("http");
const fs = require("fs");

const app = express();
app.use(express.static("public"));
app.use(express.json());
app.use(express.urlencoded({extended: true}));
app.use(session({cookie: {maxAge: 1 * 24 * 60 * 60 * 1000, secure: false}, secret: "54312"})); // HTTPS only sessions that last for 1 day
app.use(require("./api")); // Enable the API router

// const https_options = {
// 	key: fs.readFileSync("certs/selfsigned.key"),
// 	cert: fs.readFileSync("certs/selfsigned.crt"),
// }

// const server = https.createServer(https_options, app);
const server = http.createServer(app);

const PORT = process.env.PORT || 8080;
server.listen(PORT, () => {
	console.log(`running on port ${PORT}`);
});