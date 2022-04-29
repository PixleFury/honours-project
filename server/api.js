const argon2 = require("argon2");
const fs = require("fs");

const router = require("express").Router();
module.exports = router;

const { Pool } = require("pg");
const { rows } = require("pg/lib/defaults");
const pool = new Pool({
	connectionString: process.env.DATABASE_URL,
	ssl: {rejectUnauthorized: false},
});

const LIST_NAME_LEN = 3

// Init database
pool.connect((err, client, done) => {
	if (err) throw err;

	let query = `
		CREATE TABLE IF NOT EXISTS users (
			id			SERIAL PRIMARY KEY,
			username	VARCHAR(20) NOT NULL UNIQUE,
			password	CHAR(95) NOT NULL
		);

		CREATE TABLE IF NOT EXISTS products (
			barcode						CHAR(13) PRIMARY KEY,
			name						VARCHAR(100) NOT NULL,
			description					VARCHAR(2048) NOT NULL DEFAULT '',
			sold_at_tesco				BOOLEAN NOT NULL DEFAULT FALSE,
			price_tesco					NUMERIC(6, 2),
			sold_at_marks_and_spencer	BOOLEAN NOT NULL DEFAULT FALSE,
			price_marks_and_spencer		NUMERIC(6, 2),
			sold_at_asda				BOOLEAN NOT NULL DEFAULT FALSE,
			price_asda					NUMERIC(6, 2)
		);

		CREATE TABLE IF NOT EXISTS shopping_lists (
			id			SERIAL PRIMARY KEY,
			creator_id	SERIAL NOT NULL REFERENCES users(id),
			name		VARCHAR(30) NOT NULL,
			total_cost	NUMERIC(6, 2) NOT NULL DEFAULT 0,
			status		TEXT NOT NULL CHECK (status IN ('private', 'shared', 'ready_to_shop')) DEFAULT 'private'
		);

		CREATE TABLE IF NOT EXISTS shopping_list_perms (
			list_id			SERIAL NOT NULL REFERENCES shopping_lists(id) ON DELETE CASCADE,
			user_id			SERIAL NOT NULL REFERENCES users(id),
			permission		TEXT NOT NULL CHECK (permission IN ('owner', 'editor', 'shopper')),
			PRIMARY KEY(list_id, user_id)
		);

		CREATE TABLE IF NOT EXISTS shopping_list_items (
			list_id				SERIAL NOT NULL REFERENCES shopping_lists(id) ON DELETE CASCADE,
			product_barcode		CHAR(13) NOT NULL REFERENCES products(barcode),
			amount				INTEGER NOT NULL CHECK (amount > 0) DEFAULT 1,
			shop				TEXT NOT NULL CHECK (shop IN ('tesco', 'marks_and_spencer', 'asda')),
			ticked				BOOLEAN NOT NULL DEFAULT FALSE,
			PRIMARY KEY(list_id, product_barcode)
		);

		CREATE TABLE IF NOT EXISTS shopping_list_subs (
			sub_id				SERIAL PRIMARY KEY,
			list_id				SERIAL NOT NULL REFERENCES shopping_lists(id),
			product_barcode		CHAR(13) NOT NULL REFERENCES products(barcode),
			sub_amount			INTEGER NOT NULL CHECK (sub_amount > 0) DEFAULT 1,
			UNIQUE (list_id, product_barcode)
		);
	`;
	
	client.query(query, (err, res) => {if (err) throw err});
});


async function querydb(query, values, callback) {
	pool.connect((err, client, done) => {
		if (err) throw err;
		client.query(query, values, (err, res) => {
			callback(err, res);
			done();
		});
	});
}


router.post("/register", async (req, res) => {
	let username = String(req.body["username"]);
	let password = String(req.body["password"]);

	// Check username for characters and length
	if (!/^[a-zA-Z0-9_]{3,20}$/.test(username)) {
		res.sendStatus(400);
		return;
	}

	// Check password length
	if (password.length < 8) {
		res.sendStatus(400);
		return;
	}

	// hash password with argon2i - default settings are secure and salt is included
	let pw_hash = await argon2.hash(password, {type: argon2.argon2i});

	querydb("INSERT INTO users (username, password) VALUES ($1, $2) RETURNING id;", [username, pw_hash], (err, results) => {
		if (err) throw err;

		req.session.userid = results.rows[0].id;
		res.json({id: results.rows[0].id});
	});
});


router.post("/auth", async (req, res) => {
	let username = String(req.body["username"]);
	let password = String(req.body["password"]);

	querydb("SELECT id, username, password FROM users WHERE username = $1;", [username], async (err, results) => {
		if (err) throw err;

		// No matching username
		if (results.rows.length < 1) {
			res.sendStatus(401);
			return;
		}

		// Check password
		if (await argon2.verify(results.rows[0].password, password)) {
			req.session.userid = results.rows[0].id;
			res.json({id: results.rows[0].id});
		} else {
			res.sendStatus(401);
		}
	});
});


router.get("/users", (req, res) => {
	let username = String(req.query["q"] || "");

	querydb("SELECT id, username FROM users WHERE username ILIKE '%' || $1 || '%';", [username], (err, results) => {
		if (err) throw err;
		res.json(results.rows);
	});
});


router.get("/users/:userid", (req, res) => {
	let userid = String(req.params.userid);

	querydb("SELECT id, username FROM users WHERE id = $1", [userid], (err, results) => {
		if (err) throw err;

		if (results.rows.length < 1) {
			res.sendStatus(404);
		} else {
			res.json(results.rows[0]);
		}
	});
});


router.get("/products", (req, res) => {
	let query = String(req.query["q"] || "");
	let page = Number(req.query["p"] || 0).toFixed(0); // Convert to integer
//	let sort = String(req.query["sort"] || "name").toFixed(0);
//	let filter_shops = String(req.query["shops" || ""]).split(",");
	let limit = 10

	querydb(`
			SELECT * FROM products WHERE name ILIKE '%' || $1 || '%'
			ORDER BY name ASC
			LIMIT $2::int OFFSET $2::int * $3::int;
		`,
		[query, limit, page],
		(err, results) => {
		if (err) throw err;

		res.json({has_more: results.rows.length != 0, products: results.rows});
	});
});


router.get("/products/:barcode", (req, res) => {
	let barcode = String(req.params.barcode);

	querydb("SELECT * FROM products WHERE barcode = $1;", [barcode], (err, results) => {
		if (err) throw err;

		if (results.rows.length < 1) {
			res.sendStatus(404);
		} else {
			res.json(results.rows[0]);
		}
	});
});


router.get("/lists", (req, res) => {
	let query = String(req.query["q"] || "");

	querydb(`
		SELECT shopping_lists.id, shopping_lists.name, shopping_lists.total_cost, shopping_lists.status, shopping_list_perms.permission
			FROM shopping_lists
			INNER JOIN shopping_list_perms ON shopping_lists.id = shopping_list_perms.list_id
			WHERE (shopping_lists.name ILIKE '%' || $1 || '%') AND (shopping_list_perms.user_id = $2);
		`, [query, req.session.userid], (err, results) => {
		if (err) throw err;

		res.json(results.rows);
	});
});


router.post("/lists", (req, res) => {
	let name = String(req.body.name || "");

	// Logged in?
	if (req.session.userid == undefined) {
		res.status(401).send();
		return;
	}

	if (name.length < LIST_NAME_LEN) {
		res.status(400).send(`New list name must be at least ${LIST_NAME_LEN} characters long`);
		return;
	}

	querydb("INSERT INTO shopping_lists (name, creator_id) VALUES ($1, $2) RETURNING *;", [name, req.session.userid], (err, results) => {
		if (err) throw err;

		let new_list = results.rows[0];

		querydb("INSERT INTO shopping_list_perms (list_id, user_id, permission) VALUES ($1, $2, $3);", [new_list.id, new_list.creator_id, "owner"], (err, results) => {
			new_list.permission = "owner"
			res.json(new_list);
		});
	});
});


router.get("/lists/:listid", (req, res) => {
	let listid = Number(req.params.listid);
	if (isNaN(listid)) {
		res.sendStatus(400)
		return
	}
	
	// Logged in?
	if (req.session.userid == undefined) {
		res.status(401).send();
		return;
	}
	
	querydb(`
		SELECT shopping_lists.id, shopping_lists.name, shopping_lists.total_cost, shopping_lists.status, shopping_list_perms.permission
			FROM shopping_lists
			INNER JOIN shopping_list_perms ON shopping_lists.id = shopping_list_perms.list_id
			WHERE (shopping_lists.id = $1) AND (shopping_list_perms.user_id = $2);
		`, [listid, req.session.userid], (err, results) => {
		if (err) throw err;	

		if (results.rows.length < 1) {
			res.sendStatus(404);
		} else {
			let shopping_list = results.rows[0];
			querydb(`
				SELECT barcode, amount, shop, ticked,
				CASE
					WHEN shop = 'tesco' THEN price_tesco
					WHEN shop = 'marks_and_spencer' THEN price_marks_and_spencer
					WHEN shop = 'asda' THEN price_asda
					ELSE 0
				END AS unit_price
				FROM shopping_list_items INNER JOIN products ON product_barcode = barcode
				WHERE list_id = $1;
			`, [listid], (err, results) => {
				if (err) throw err;

				shopping_list.items = results.rows;
				res.json(shopping_list);
			});
		}
	});
});

router.put("/lists/:listid/items", (req, res) => {
	let list_id = String(req.params.listid);

	if (!("barcode" in req.body && "shop" in req.body && "amount" in req.body)) {
		res.sendStatus(400);
		return;
	}

	let barcode = String(req.body["barcode"]);
	let shop = String(req.body["shop"]);
	let amount = Number(req.body["amount"]);

	if (amount < 1) {
		querydb("DELETE FROM shopping_list_items WHERE list_id = $1 AND product_barcode = $2;", [list_id, barcode], (err, results) => {
			if (err) throw err;
			res.json({status: 200});
		});
	} else {
		querydb(`
			INSERT INTO shopping_list_items (list_id, product_barcode, shop, amount)
				VALUES ($1, $2, $3, $4)
				ON CONFLICT (list_id, product_barcode)
				DO UPDATE SET shop = EXCLUDED.shop, amount = EXCLUDED.amount;
			`, [list_id, barcode, shop, amount], (err, results) => {
			if (err) throw err;
			res.json({status: 200});
		});
	}

	querydb(`
		UPDATE shopping_lists SET total_cost = total FROM (
			SELECT COALESCE(SUM(
				CASE
					WHEN shop = 'tesco' THEN price_tesco
					WHEN shop = 'marks_and_spencer' THEN price_marks_and_spencer
					WHEN shop = 'asda' THEN price_asda
					ELSE 0
				END * amount), 0) AS total
			FROM shopping_list_items INNER JOIN products ON product_barcode = barcode
		) AS sum WHERE shopping_lists.id = $1
		`, [list_id], (err, results) => {
		if (err) throw err;
	});
});


router.put("/lists/:listid/ticked", (req, res) => {
	let list_id = String(req.params.listid);

	if (!("barcode" in req.body || "ticked" in req.body)) {
		res.sendStatus(400);
		return;
	}

	let barcode = String(req.body["barcode"]);
	let ticked = Number(req.body["ticked"]);

	querydb("UPDATE shopping_list_items SET ticked = $3 WHERE list_id = $1 AND product_barcode = $2;", [list_id, barcode, ticked], (err, results) => {
		if (err) throw err;
		res.json({status: 200})
	});
});


// router.put("/lists/:listid/subs", (req, res) => {
// 	let list_id = String(req.params.listid);

// 	if (!("barcode" in req.body && "shop" in req.body && "amount" in req.body)) {
// 		res.sendStatus(400);
// 		return;
// 	}

// 	let barcode = String(req.body["barcode"]);
// 	let shop = String(req.body["shop"]);
// 	let amount = Number(req.body["amount"]);

// 	if (amount < 1) {
// 		res.sendStatus(400);
// 		return;
// 	}

// 	querydb(`
// 		INSERT INTO shopping_list_items (list_id, product_barcode, shop, sub_amount)
// 			VALUES ($1, $2, $3, $4)
// 			ON CONFLICT (list_id, product_barcode)
// 			DO UPDATE SET shop = EXCLUDED.shop, sub_amount = EXCLUDED.sub_amount;
// 		`, [list_id, barcode, shop, amount], (err, results) => {
// 		if (err) throw err;
// 		res.json({status: 200})
// 	});
// });


router.put("/lists/:listid/users", (req, res) => {
	let list_id = String(req.params.listid);

	let user_id = String(req.body["user_id"]);
	let permission = String(req.body["permission"]);

	// Don't edit own permissions
	if (user_id == req.session.userid) {
		res.sendStatus(401);
		return;
	}

	// Check user owns list
	querydb("SELECT list_id FROM shopping_list_perms WHERE list_id = $1 AND user_id = $2 AND permission = 'owner';", [list_id, req.session.userid], (err, results) => {
		if (err) throw err;

		console.log(results.rows);

		if (rows.length < 1) {
			res.sendStatus(401);
			return;
		}

		querydb(`
			INSERT INTO shopping_list_perms (list_id, user_id, permission)
				VALUES ($1, $2, $3)
				ON CONFLICT (list_id, user_id)
				DO UPDATE SET permission = EXCLUDED.permission;
			`, [list_id, user_id, permission], (err, results) => {
			if (err) throw err;

			// TODO: combine this query with the previous one
			querydb("SELECT id, username FROM users WHERE id = $1", [user_id], (err, results) => {
				if (err) throw err;

				res.json(results.rows[0]);
			})

			querydb("UPDATE shopping_lists SET status = $2 WHERE id = $1", [list_id, "shared"], (err, results) => {
				if (err) throw err;
			});
		});
	});
});

router.delete("/lists/:listid", (req, res) => {
	let list_id = String(req.params.listid);

	querydb("DELETE FROM shopping_lists WHERE id = $1 AND creator_id = $2;", [list_id, req.session.userid], (err, results) => {
		if (err) throw err;
		res.json({status: 200});
	});
});