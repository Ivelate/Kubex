# Kubex

Voxel Graphic Engine with eh, cubes. Final Computer Science degree project by **Víctor Arellano Vicente** (Ivelate). Java / LWJGL / OpenGL

For now, it implements:

- Glorious 3D cubes
- Optimization via culling: Supports up to 2500 32x32x32 simultaneous chunks in screen without lagging significantly (81.920.000  cubes, more or less. Awesome.)
- 3D "infinite" procedural world (Using a 3D perlin noise function). The world loads dinamically around the player position. Total map width/length ~= 4 million km, area ~= 16 trillion km^2 (Almost 32.000 times the area of the earth) . Upon this place integers overflow and some weird (Fun) things happen.
- Lightning (Artificial and natural light, using a celular automata)
- Water shaders. Real-time reflections. Reallistic light absorption based on distance. Fresnel. Screen space reflections / ray marching.
- Complex shadows: Shadow mapping with cascade shadow mapping for multi-distance shadow precision. Culling on shadows, too.
- EXPLOSIONS (Not optimized yet though)
- Multithreading for chunk creation / updates, so map loading doesn't affect game FPS in any way.
- Day-night cycle (Atmospheric Scattering)
- Cool sunsets.
- Complex HUD: A gray point in the center of the screen
- Collisions, walking, swimming, flying
- Cascaded Shadow Mapping (Good quality shadows in all distances of the scene)
- Realistic water
- Mip mapping / Anisotropic Filtering
- EXPLOSIONS

And much more yet to come!

Do you want to play? Download the .jar from https://github.com/Ivelate/Kubex/releases/tag/1.0
