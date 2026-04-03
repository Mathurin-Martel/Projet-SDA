import osmnx as ox;
import pandas as pd

graphe = ox.graph_from_place("Paris, France", network_type="drive");
contenu = [];
limite=0;
#ox.plot_graph(graphe);
for x, y, z, data in graphe.edges(keys=True, data=True):
    if limite<=10: #Changer ce nombre ou enlever la condition pour tous prendre
        contenu.append([x, y, data["length"], data.get("name")]);
        limite=limite+1;
    else : 
        exit;


Data = pd.DataFrame(contenu, columns=["Depuis", "Vers", "Distance", "Nom"]);
print(Data);
