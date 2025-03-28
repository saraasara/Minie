/*
 Copyright (c) 2024 Stephen Gold
 All rights reserved.

 Redistribution and use in source and binary forms, with or without
 modification, are permitted provided that the following conditions are met:
 * Redistributions of source code must retain the above copyright
 notice, this list of conditions and the following disclaimer.
 * Redistributions in binary form must reproduce the above copyright
 notice, this list of conditions and the following disclaimer in the
 documentation and/or other materials provided with the distribution.
 * Neither the name of the copyright holder nor the names of its contributors
 may be used to endorse or promote products derived from this software without
 specific prior written permission.

 THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.github.stephengold.shapes.custom;

import com.jme3.bullet.collision.shapes.CustomConvexShape;
import com.jme3.export.InputCapsule;
import com.jme3.export.JmeExporter;
import com.jme3.export.JmeImporter;
import com.jme3.export.OutputCapsule;
import com.jme3.math.Vector3f;
import java.io.IOException;
import java.util.logging.Logger;
import jme3utilities.Validate;
import jme3utilities.math.MyMath;
import jme3utilities.math.MyVolume;

/**
 * A collision shape for a solid right circular cylinder with uniform density.
 * By convention, the local Y axis is the height axis.
 * <p>
 * {@code CylinderCollisionShape} is more flexible and probably more efficient.
 * <p>
 * Unlike {@code CylinderCollisionShape}, this is an imprecise shape; margin
 * always expands the shape.
 *
 * @author Stephen Gold sgold@sonic.net
 */
public class CustomCylinder extends CustomConvexShape {
    // *************************************************************************
    // constants and loggers

    /**
     * message logger for this class
     */
    final public static Logger loggerZ
            = Logger.getLogger(CustomCylinder.class.getName());
    /**
     * field names for serialization
     */
    final private static String tagUnscaledHeight = "unscaledHeight";
    final private static String tagUnscaledRadius = "unscaledRadius";
    // *************************************************************************
    // fields

    /**
     * scaled height, excluding margin (in physics-space units)
     */
    private float scaledHeight;
    /**
     * scaled base radius, excluding margin (in physics-space units)
     */
    private float scaledRadius;
    /**
     * height, for scale=(1,1,1) and margin=0
     */
    private float unscaledHeight;
    /**
     * base radius, for scale=(1,1,1) and margin=0
     */
    private float unscaledRadius;
    // *************************************************************************
    // constructors

    /**
     * No-argument constructor needed by SavableClassUtil.
     */
    protected CustomCylinder() {
    }

    /**
     * Instantiate a cylinder with the specified dimensions.
     *
     * @param radius the desired base radius, before scaling and excluding
     * margin (&gt;0)
     * @param height the desired height, before scaling and excluding margin
     * (&gt;0)
     */
    public CustomCylinder(float radius, float height) {
        super(radius, 0.5f * height, radius);

        Validate.positive(radius, "radius");
        Validate.positive(height, "height");

        this.unscaledHeight = height;
        this.unscaledRadius = radius;
        setScale(scale);
    }
    // *************************************************************************
    // new methods exposed

    /**
     * Copy the half extents of the cylinder.
     *
     * @param storeResult storage for the result (modified if not null)
     * @return the unscaled half extent for each local axis (either storeResult
     * or a new vector, not null, no negative component)
     */
    public Vector3f getHalfExtents(Vector3f storeResult) {
        Vector3f result = (storeResult == null) ? new Vector3f() : storeResult;
        result.x = unscaledRadius;
        result.y = 0.5f * unscaledHeight;
        result.z = unscaledRadius;

        return result;
    }

    /**
     * Return the height of the cylinder.
     *
     * @return the unscaled height (&gt;0)
     */
    public float getHeight() {
        assert unscaledHeight > 0f : unscaledHeight;
        return unscaledHeight;
    }

    /**
     * Return the radius of the cylinder.
     *
     * @return the unscaled radius (&gt;0)
     */
    public float getRadius() {
        assert unscaledRadius > 0f : unscaledRadius;
        return unscaledRadius;
    }
    // *************************************************************************
    // CustomConvexShape methods

    /**
     * Test whether the specified scale factors can be applied to this shape.
     * For a cylinder, scaling must preserve the circular cross section.
     *
     * TODO allow general scaling
     *
     * @param scale the desired scale factor for each local axis (may be null,
     * unaffected)
     * @return true if applicable, otherwise false
     */
    @Override
    public boolean canScale(Vector3f scale) {
        boolean result = super.canScale(scale) && scale.x == scale.z;
        return result;
    }

    /**
     * Locate the shape's supporting vertex for the specified normal direction,
     * excluding collision margin.
     * <p>
     * This method is invoked by native code.
     *
     * @param dirX the X-coordinate of the direction to test (in scaled shape
     * coordinates)
     * @param dirY the Y-coordinate of the direction to test (in scaled shape
     * coordinates)
     * @param dirZ the Z-coordinate of the direction to test (in scaled shape
     * coordinates)
     * @return the location on the shape's surface with the specified normal (in
     * scaled shape coordinates, must lie on or within the shape's bounding box)
     */
    @Override
    protected Vector3f locateSupport(float dirX, float dirY, float dirZ) {
        Vector3f result = threadTmpVector.get();

        // The supporting vertex lies on the rim of one of the bases:
        result.y = ((dirY < 0f) ? -0.5f : 0.5f) * scaledHeight; // which base
        float dxz = MyMath.hypotenuse(dirX, dirZ);
        if (dxz == 0f) { // avoid division by zero
            result.x = scaledRadius;
            result.z = 0f;

        } else {
            result.x = scaledRadius * (dirX / dxz);
            result.z = scaledRadius * (dirZ / dxz);
        }

        return result;
    }

    /**
     * Calculate how far the scaled shape extends from its center of mass,
     * excluding collision margin.
     *
     * @return the distance (in physics-space units, &ge;0)
     */
    @Override
    public float maxRadius() {
        float halfHeight = 0.5f * scaledHeight;
        float result = MyMath.hypotenuse(scaledRadius, halfHeight);

        return result;
    }

    /**
     * De-serialize this shape from the specified importer, for example when
     * loading from a J3O file.
     *
     * @param importer (not null)
     * @throws IOException from the importer
     */
    @Override
    public void read(JmeImporter importer) throws IOException {
        super.read(importer);
        InputCapsule capsule = importer.getCapsule(this);

        this.unscaledHeight = capsule.readFloat(tagUnscaledHeight, 1f);
        this.unscaledRadius = capsule.readFloat(tagUnscaledRadius, 1f);

        setScale(scale);
    }

    /**
     * Estimate the volume of the collision shape, including scale and margin.
     *
     * @return the estimated volume (in physics-space units cubed, &ge;0)
     */
    @Override
    public float scaledVolume() {
        float r = scaledRadius + margin;
        float h = scaledHeight + 2f * margin;
        float result = MyVolume.cylinderVolume(new Vector3f(r, h, r));

        return result;
    }

    /**
     * Alter the scale of the shape.
     * <p>
     * Note that if shapes are shared (between collision objects and/or compound
     * shapes) changes can have unintended consequences.
     *
     * @param scale the desired scale factor for each local axis (not null, no
     * negative component, unaffected, default=(1,1,1))
     */
    @Override
    public void setScale(Vector3f scale) {
        super.setScale(scale);

        // super.setScale() has verified that scale.x == scale.z
        this.scaledHeight = scale.y * unscaledHeight;
        this.scaledRadius = scale.x * unscaledRadius;

        float hSquared = scaledHeight * scaledHeight;
        float rSquared = scaledRadius * scaledRadius;
        /*
         * the moments of inertia of a uniformly dense right circular cylinder
         * with mass=1, around its center of mass:
         */
        float ixz = rSquared / 4f + hSquared / 12f;
        float iy = 0.5f * rSquared;
        setScaledInertia(ixz, iy, ixz);
    }

    /**
     * Serialize this shape to the specified exporter, for example when saving
     * to a J3O file.
     *
     * @param exporter (not null)
     * @throws IOException from the exporter
     */
    @Override
    public void write(JmeExporter exporter) throws IOException {
        super.write(exporter);
        OutputCapsule capsule = exporter.getCapsule(this);

        capsule.write(unscaledHeight, tagUnscaledHeight, 1f);
        capsule.write(unscaledRadius, tagUnscaledRadius, 1f);
    }
}
