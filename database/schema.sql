CREATE TABLE zones (id SERIAL PRIMARY KEY, name VARCHAR(50) NOT NULL, hourly_rate NUMERIC(8,2) NOT NULL);
CREATE TABLE workstations (id SERIAL PRIMARY KEY, zone_id INT NOT NULL REFERENCES zones(id), name VARCHAR(50) NOT NULL);
CREATE TABLE clients (id SERIAL PRIMARY KEY, name VARCHAR(100) NOT NULL, phone VARCHAR(20));
CREATE TABLE reservations (id SERIAL PRIMARY KEY,
  client_id INT NOT NULL REFERENCES clients(id) ON DELETE CASCADE,
  workstation_id INT NOT NULL REFERENCES workstations(id) ON DELETE CASCADE,
  start_time TIMESTAMP NOT NULL DEFAULT now(),
  end_time TIMESTAMP,
  status VARCHAR(20) NOT NULL DEFAULT 'active',
  total_cost NUMERIC(10,2));
CREATE UNIQUE INDEX one_active_reservation ON reservations(workstation_id) WHERE status = 'active';
INSERT INTO zones (name, hourly_rate) VALUES ('Standard', 150), ('VIP', 300), ('PlayStation', 250);
INSERT INTO workstations (zone_id, name) VALUES (1,'PC-01'),(1,'PC-02'),(1,'PC-03'),(2,'VIP-01'),(2,'VIP-02'),(3,'PS5-01');
INSERT INTO clients (name, phone) VALUES ('Иван Петров','+7-916-111-22-33'),('Анна Смирнова','+7-925-444-55-66'),('Дмитрий Козлов','+7-903-777-88-99');
INSERT INTO reservations (client_id, workstation_id) VALUES (1,4),(2,6);
