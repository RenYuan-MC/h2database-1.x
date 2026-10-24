-- Fixture for the browser smoke test (test/index.html).
--
-- test/fixture/smoke14.mv.db is a 1.4.200-format MVStore database. It was created
-- with the release h2-1.4.200.jar, which is not committed. To regenerate it, run
-- from the wasm project root:
--
--   java -cp h2-1.4.200.jar org.h2.tools.RunScript `
--     '-url' 'jdbc:h2:./test/fixture/smoke14' '-user' 'sa' `
--     '-script' 'test/fixture/create.sql'
--
CREATE TABLE IF NOT EXISTS users(
    id INT PRIMARY KEY,
    name VARCHAR(64) NOT NULL,
    score BIGINT DEFAULT 0
);
INSERT INTO users VALUES (1, 'Steve', 100), (2, 'Alex', 250), (3, 'Herobrine', 0);
CREATE TABLE IF NOT EXISTS meta(
    k VARCHAR(32) PRIMARY KEY,
    v VARCHAR(255)
);
INSERT INTO meta VALUES ('schema', '14'), ('generatedBy', 'h2-1x-wasm smoke test');
