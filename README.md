# Projekt Stadtbaum

## Übersicht

Dieses Repository enthält folgende Module:

1. Android-App zum Anzeigen von Websites wenn sich ein bestimmtes Bluetooth-Gerät in Reichweite befindet.
2. JSON-Datei mit den bekannten Geräten und deren anzuzeigende Website.
3. Websites mit Erklärungen, Statistiken etc. zu den verschiedenen Stadtbäumen.

***

## 1. Android-App

Übernimt die Suche nach Bluetooth Low Energy-Geräten, verarbeitet das JSON-Mapping und zeigt die 
entsprechende Wesite an.


## 2. JSON-Mapping

Enthält eine JSON-Liste aus beliebig vielen Elementen, welche die Eigenschaften `address` und
`display_url`. Die `address`-Eigenschaft bezeichnet die öffentliche "MAC"-Adresse des Bluetooth-
Geräts (Beaon). `display_url` ist die dafür anzuzeigende Website (eine absolute URL ist notwendig).

Befindet sich unter `/webresources/devicemapping.json`.


## 3. Websites

Enthalten Informationen, Erklärungen oder/und Statistiken zu allen Bäumen im Mapping.

Befinden sich unter `/webresources/[ordnername]`.