db.createUser({
    user: 'mongo',
    pwd: 'mongo',
    roles: [
        {
            role: 'root',
            db: 'admin',
        },
    ],
});

db = db.getSiblingDB('application');

db.book.insert([
    { "id": 10, "title": "The Martian" },
    { "id": 11, "title": "Blue Mars" },
    { "id": 12, "title": "The Case for Mars" },
    { "id": 13, "title": "The War of Worlds" },
    { "id": 14, "title": "Edison''s Conquest of Mars "}
]);