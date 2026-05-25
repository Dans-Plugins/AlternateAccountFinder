# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/).

## [Unreleased]

## [2.0.0]

### Changed

- Rewrote the plugin for Spigot 1.17+ (v2 by Ren Binden).
- Migrated data layer to jOOQ with Flyway migrations.
- Added HikariCP connection pooling.
- Added support for MariaDB in addition to the embedded H2 database.
- Added integration with the Mailboxes and RPKit notification systems.
