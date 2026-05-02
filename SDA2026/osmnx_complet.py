import osmnx as ox;
import pandas as pd;
import csv;

graphe = ox.graph_from_place("Paris, France", network_type="drive");
graphe_metres=ox.project_graph(graphe);

nodes = list(graphe_metres.nodes(data=True))

with open('coordinates_osmnx.csv', "w", newline='') as fichier:
    writer = csv.writer(fichier, delimiter=';');
    writer.writerow(["Intersection", "X", "Y"]);
    for node_id, data in nodes:
        writer.writerow([node_id, data['x'], data['y']])

contenu = [];

for x, y, z, data in graphe_metres.edges(keys=True, data=True):
        contenu.append([x, y, data["length"], data.get("name")]);

Data = pd.DataFrame(contenu, columns=["Depuis", "Vers", "Distance", "Nom"]);

rows_number=len(Data);

with open('donnees_osmnx.csv', "w", encoding="utf-8", newline='') as fichier:
    writer = csv.writer(fichier, delimiter=';');
    writer.writerow(["Depuis", "Vers", "Distance", "Nom"]);
    for row in range(rows_number):
        writer.writerow(Data.iloc[row]);