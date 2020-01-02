import matplotlib.pyplot as plt
import numpy as np

x, y = np.loadtxt('results.txt', delimiter=',', unpack=True)

circle = plt.Circle((0,0), radius=40, fill=False)

ax = plt.gca()
ax.add_patch(circle)
plt.axis('scaled')

plt.scatter(x,y)

plt.xlabel('x')
plt.ylabel('y')
plt.title('InitialPositionConnectedRing Graphical representation\n')
plt.legend()
plt.show()
fig.savefig('InitialPositionConnectedRing.png')
