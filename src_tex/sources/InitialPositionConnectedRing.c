#include <stdio.h>
#include <math.h>

#define MAX(x, y) (((x) > (y)) ? (x) : (y))
#define MIN(x, y) (((x) < (y)) ? (x) : (y))

int main(void)
{
	int rayon_even;
	int rayon_odd;
	double delta_angle;

	int module;
	int angle;

	int size = 10;
	int scope = 32;
	int maxY = 500;

	// coordonnees de base
	int x = 0;
	int y = 0;

	// coordonnees suivantes
	int next_x, next_y;

	rayon_odd = MIN(size * (scope / 8), (maxY / 2.5));
	rayon_even = rayon_odd - (scope / 2);

	printf("%s : odd rayon = %d\n", __func__, rayon_odd);
	printf("%s : even rayon = %d\n", __func__, rayon_even);

	delta_angle = 2 * M_PI / size;

	// angle = delta*host_id, module = rayon
	for (int host_id = 0; host_id < 10; host_id++)
	{
		angle = delta_angle * host_id;

		if (host_id % 2 == 0) {
			module = rayon_odd;
		} else {
			module = rayon_even;
		}

		next_x = sin(angle) * module + x;
		next_y = cos(angle) * module + y;

		/*printf("%s : x%d = (%d, %d)\n", __func__, host_id, next_x, next_y);*/
		printf("%d, %d\n", next_x, next_y);
	}

	return 0;
}
