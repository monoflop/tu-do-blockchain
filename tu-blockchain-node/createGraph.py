import pandas as pd
from matplotlib import pyplot as plt
import matplotlib.patches as mpatches
from matplotlib.lines import Line2D

plt.rcParams["figure.figsize"] = [7.00, 3.50]
plt.rcParams["figure.autolayout"] = True
columns = ["time", "block", "type"]
df = pd.read_csv("simulation.csv", usecols=columns)

df.set_index('time', inplace=True)
df.groupby('type')['block'].plot(drawstyle="steps", legend=True)

handles, labels = plt.gca().get_legend_handles_labels() 
line1 = Line2D([0], [0], label='Startblock', color='b', linestyle=(0, (5,10)), linewidth="0.5")
line2 = Line2D([0], [0], label='Verifikation z=8', color='r', linestyle=(0, (5,10)), linewidth="0.5")

handles.extend([line1, line2])
plt.legend(handles=handles)

plt.axhline(y=2, color='b', linestyle=(0, (5,10)), linewidth="0.5")
plt.axhline(y=10, color='r', linestyle=(0, (5,10)), linewidth="0.5")
plt.xlabel('Simulationszeit (in ms.)')
plt.ylabel('Blockchainlänge')
plt.show()