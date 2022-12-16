# Java Blockchain
Experimentelle Implementierung einer Blockchain mit Smart-Contract-Unterstützung.

## Kontext
Im Laufe meiner Bachelorarbeit "Smart Contracts and Blockchains" habe ich mich experimentell den Grundlagen der Blockchain-Technologie gewidmet und eine eigene Blockchain von Grund auf implementiert. Konzeptionell habe ich mich dabei an der ursprünglichen Implementierung von Bitcoin orientiert. Zusätzlich zu der Funktionalität als Kryptowährung soll die Blockchain auch simple Smart Contracts unterstützen.

## Features und Eigenschaften
### Netzwerk
- Peer-to-Peer über TCP Sockets
- JSON basiertes Protokoll
- Nodes senden sich gegenseitig bereits bekannte und gültige Nodes
- Alle Nodes sind untereinander verbunden
- Broadcasts und Kommunikation mit einzelnen Nodes

### Blockchain
- Proof of Work
- SHA256 Block-Hash
- RSA-Public-Key-Verschlüsselung
- RSA-SHA256 Signaturen

### Kryptowährung
- UTXO Modell (Mit mehreren Inputs und Outputs)
- Bitcoin ähnliches Pay-to-PubKey
- Blockreward für erstellte Blöcke
- Wallets für Schlüsselpaare

### Smart Contracts
TODO

## How-To
### JAR erstellen
Software erforderlich: java (17), maven, openssl
```
mvn package
```

### Wallet erstellen
```
java -jar tu-blockchain-node-1.0-SNAPSHOT.jar gen-wallet --out test.wallet
```

### Genesis Block erstellen
Genesis Block minen und in einer neuen Blockchaindatei speichern.
```
java -jar tu-blockchain-node-1.0-SNAPSHOT.jar gen-genesis --out blockchain.json
```

### Node konfigurieren und starten
Im Node-Modus verhält sich das Programm als Teil des Blockchainnetzwerks:
- Verbindung mit anderen Nodes
- Blockchain synchronisierung
- Neue Blöcke werden erstellt
- Es kann mit dem Netzwerk interagiert werden:
    - Transaktionen erstellen
    - UTXO finden und verfügbare Coins summieren
    - Blockchain inspizieren

#### Konfigurieren
Im Node-Modus wird Folgendes benötigt:
- Wallet
- Blockchaindatei mit mindestens dem Genesis Block
- Konfigurationsdatei

Beispiel Arbeitsverzeichnis:
```
node.wallet
blockchain.json
config.json
```

Beispiel Konfigurationsdatei:

Die erste Node, die gestartet wird, benötigt keine knownPeers. Alle weiteren Nodes benötigen nicht zwangsweise alle anderen Nodes, da beim Verbindungsaufbau alle bekannten Nodes ausgetauscht werden.
```
{
  "name":"node1",
  "port":8000,
  "walletFilePath":"node1.wallet",
  "blockchainFilePath":"blockchain.json",
  "knownPeers": [
    {
      "ip":"127.0.0.1",
      "port": 8000
    }
  ]
}
```

#### Starten
```
java -jar tu-blockchain-node-1.0-SNAPSHOT.jar --directory testingNodes/node1 node
```

### Node interaktion
Wenn eine Node erfolgreich gestartet wurde, können folgende Befehle ausgeführt werden:
```
nodes                 // Zeigt eine Liste mit allen verbundenen Nodes
connect [ip] [port]   // Stellt eine Verbindung mit einer neuen Node her
ping                  // Sendet eine Ping-Nachricht an alle anderen Nodes
save                  // Speichert die aktuelle Blockchain in blockchain.json
balance-key [pubKey]  // Findet alle Transaktion mit UTXO
balance [wallet]      // s.o
transactions [pubKey] // Zeigt alle Transaktionen an, in denen der Schlüssel involviert ist

tx [txId] [vOut] [amount] [target pubKey] [wallet] // Erstellt eine neue Transaktion. Es kann ein paar Blöcke dauern, bis die Transaktion aufgenommen wurde
```

### Test-Umgebung
Im Ordner testingNodes/ sind bereits vier Nodes vorkonfiguriert und können direkt getestet werden.

## Mängel oder Schwachstellen
- Netzwerk eher weniger skalierbar, da alle Nodes miteinander verbunden sind
- Signaturen basieren auf mit Gson serialisierten JSON Objekten. Gson garantiert keine Feldreihenfolge. Die Reihenfolge basiert auf der Reihenfolge, die durch Java Reflections zurückgegeben wird. Diese ist leider nicht garantiert immer identisch.
- Gson kann nicht garantieren, dass alle Felder eines deserialisierten Objekts existieren (Nicht deserialisierbare Felder werden mit null initialisiert)
- Asynchrone Programmierung fehlerhaft
- Tests

## Hinweis
Die Implementierung ist auf experimenteller Basis erfolgt. Die Blockchain ist vermutlich weder sicher noch frei von Bugs.