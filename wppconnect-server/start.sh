#!/bin/sh

echo "Limpando locks do Chromium..."
rm -f /usr/src/wpp-server/tokens/*/Default/SingletonLock
rm -f /usr/src/wpp-server/userDataDir/*/Default/SingletonLock
rm -f /usr/src/wpp-server/userDataDir/*/SingletonLock

echo "Iniciando o WPPConnect Server..."
exec "$@"
