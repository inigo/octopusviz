octopus-energyviz retrieves gas and electricity usage from Octopus Energy.

### Usage

This is a normal sbt project. You can compile code with `sbt compile`, run it with `sbt run`, and `sbt console` will start a Scala 3 REPL.

## Postgres configuration

Ensure you have Postgres 13+ installed and running.

Create the required database - e.g. via:

```bash
    > psql -h localhost -U postgres
```

then:

```sql
    create user "energy" with password 'password';
    create database "energy" with owner "energy";
    alter user energy with SUPERUSER;
    quit
```
