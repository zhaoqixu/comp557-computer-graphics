Zhaoqi Xu 260563752

All images are named the same as the corresponding xml files.

The novel scene is defined in NewScene.xml and the rendered image is named as NewScene.png. This scene contains one cube, two spheres and a bunny mesh inside a cornell box with white and black tiles floor.

Extra features: Mirror reflection and Fresnel reflection. Fresnel reflection is demonstracted in TwoSpheresPlane_FR.png where the black and white tile is reflected on the bottom of red and blue sphere. Mirror reflection is demonstrated in the NewScene.png where everything is reflected on the sphere on the right, while the material of the light yellow shpere on the left can do Fresnel reflection. 

NOTE: To enable the Mirror and Fresnel reflection feature, you have to set a boolean variable reflect in Render class first. You can do this by simply uncomment line 116 in Scene.java file.

AACeckerPlane.png
Implemented as instructed, 100% matches the reference.

BoxRGBLights.png
Implemented as instructed, 100% matches the reference.

BoxStacks.png
Implemented as instructed, 100% matches the reference.

BunnyMesh.png
To test the triangle meshes, rendered the bunny.obj.

Cornell.png
Implemented as instructed, 100% matches the reference.

Plane.png
Implemented as instructed, 100% matches the reference.

Plane2.png
Implemented as instructed, 100% matches the reference.

Sphere.png
Implemented as instructed, 100% matches the reference.

TorusMesh.png
Implemented as instructed, 100% matches the reference.

TwoSpheresPlane.png
Implemented as instructed, 100% matches the reference.

TwoSpheresPlane_FR.png
Render the spheres using Fresnel Reflectable material(config the Render.reflect = true).

NewScene.png
Objective 11, details is explained above.