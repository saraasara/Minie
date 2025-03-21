/*
 Copyright (c) 2018-2024 Stephen Gold
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
package jme3utilities.minie.test;

import com.jme3.asset.AssetManager;
import com.jme3.asset.DesktopAssetManager;
import com.jme3.bullet.PhysicsSoftSpace;
import com.jme3.bullet.PhysicsSpace;
import com.jme3.bullet.RotationOrder;
import com.jme3.bullet.SoftBodyWorldInfo;
import com.jme3.bullet.collision.AfMode;
import com.jme3.bullet.collision.PhysicsCollisionObject;
import com.jme3.bullet.collision.shapes.CollisionShape;
import com.jme3.bullet.collision.shapes.SphereCollisionShape;
import com.jme3.bullet.control.RigidBodyControl;
import com.jme3.bullet.control.SoftBodyControl;
import com.jme3.bullet.control.VehicleControl;
import com.jme3.bullet.joints.New6Dof;
import com.jme3.bullet.joints.PhysicsJoint;
import com.jme3.bullet.joints.Point2PointJoint;
import com.jme3.bullet.objects.PhysicsBody;
import com.jme3.bullet.objects.PhysicsRigidBody;
import com.jme3.bullet.objects.PhysicsSoftBody;
import com.jme3.bullet.objects.PhysicsVehicle;
import com.jme3.bullet.objects.infos.ConfigFlag;
import com.jme3.bullet.objects.infos.Sbcp;
import com.jme3.bullet.objects.infos.SoftBodyConfig;
import com.jme3.bullet.objects.infos.SoftBodyMaterial;
import com.jme3.bullet.util.NativeSoftBodyUtil;
import com.jme3.export.binary.BinaryExporter;
import com.jme3.math.Matrix3f;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.debug.WireBox;
import com.jme3.system.NativeLibraryLoader;
import jme3utilities.Heart;
import jme3utilities.math.MyQuaternion;
import org.junit.Assert;
import org.junit.Test;

/**
 * Test cloning/saving/loading/rebuilding on subclasses of PhysicsBody.
 *
 * @author Stephen Gold sgold@sonic.net
 */
public class TestCloneBody {
    // *************************************************************************
    // fields

    /**
     * AssetManager for {@code BinaryExporter.saveAndLoad()}
     */
    final private static AssetManager assetManager = new DesktopAssetManager();
    /**
     * collision shape for generating rigid bodies
     */
    private static CollisionShape shape;
    /**
     * wire mesh for generating soft bodies
     */
    final private static Mesh wireBox = new WireBox();
    /**
     * container for bodies
     */
    private static PhysicsSoftSpace space;
    // *************************************************************************
    // new methods exposed

    /**
     * Test cloning/saving/loading/rebuilding on subclasses of PhysicsBody. TODO
     * add joints
     */
    @Test
    public void testCloneBody() {
        NativeLibraryLoader.loadNativeLibrary("bulletjme", true);
        shape = new SphereCollisionShape(1f);
        space = new PhysicsSoftSpace(PhysicsSpace.BroadphaseType.DBVT);

        // Iterate in the hope of exercising the garbage collector.
        for (int iteration = 0; iteration < 9; ++iteration) {
            clonePrb();
            cloneRbc();
            clonePv();
            cloneVc();
            clonePsb();
            cloneSbc();
        }
    }
    // *************************************************************************
    // private methods

    /**
     * Clone rigid bodies.
     */
    private static void clonePrb() {
        // static
        PhysicsRigidBody body0 = new PhysicsRigidBody(shape, 0f);
        setParameters(body0, 0f);
        verifyParameters(body0, 0f);
        PhysicsRigidBody body0Clone = Heart.deepCopy(body0);
        Assert.assertTrue(body0Clone.isStatic());
        Assert.assertEquals(0f, body0Clone.getMass(), 0f);
        Assert.assertEquals(0, body0Clone.countIgnored());
        cloneTest(body0, body0Clone);

        // kinematic with mass=0
        PhysicsRigidBody k0 = new PhysicsRigidBody(shape, 0f);
        k0.setKinematic(true);
        setParameters(k0, 0f);
        verifyParameters(k0, 0f);
        PhysicsRigidBody k0Clone = Heart.deepCopy(k0);
        Assert.assertTrue(k0Clone.isKinematic());
        Assert.assertEquals(0f, k0Clone.getMass(), 0f);
        Assert.assertEquals(0, k0Clone.countIgnored());
        cloneTest(k0, k0Clone);

        // dynamic with mass=1
        PhysicsRigidBody body = new PhysicsRigidBody(shape, 1f);
        setParameters(body, 0f);
        verifyParameters(body, 0f);
        PhysicsRigidBody bodyClone = Heart.deepCopy(body);
        Assert.assertTrue(bodyClone.isDynamic());
        Assert.assertEquals(1f, bodyClone.getMass(), 0f);
        Assert.assertEquals(0, bodyClone.countIgnored());
        cloneTest(body, bodyClone);

        // kinematic with mass=2
        PhysicsRigidBody body2 = new PhysicsRigidBody(shape, 2f);
        body2.setKinematic(true);
        setParameters(body2, 0f);
        verifyParameters(body2, 0f);
        PhysicsRigidBody body2Clone = Heart.deepCopy(body2);
        Assert.assertTrue(body2Clone.isKinematic());
        Assert.assertEquals(2f, body2Clone.getMass(), 0f);
        Assert.assertEquals(0, body2Clone.countIgnored());
        cloneTest(body2, body2Clone);

        clonePrbPair();
    }

    /**
     * Clone connected pairs of rigid bodies.
     */
    private static void clonePrbPair() {
        // 2 joined dynamic bodies that ignore each another
        PhysicsRigidBody body7 = new PhysicsRigidBody(shape, 7f);
        setParameters(body7, 0f);
        PhysicsRigidBody body8 = new PhysicsRigidBody(shape, 8f);
        setParameters(body8, 0.3f);
        PhysicsJoint p2p = new Point2PointJoint(
                body7, body8, new Vector3f(), new Vector3f());
        body8.addToIgnoreList(body7);

        PhysicsRigidBody body7Clone = Heart.deepCopy(body7);
        Utils.cloneTest(body7, body7Clone);
        Assert.assertTrue(body7Clone.isDynamic());
        Assert.assertEquals(7f, body7Clone.getMass(), 0f);
        Assert.assertEquals(1, body7Clone.countIgnored());
        verifyParameters(body7Clone, 0f);

        PhysicsJoint[] joints7Clone = body7Clone.listJoints();
        Assert.assertEquals(1, joints7Clone.length);
        PhysicsJoint p2pClone = joints7Clone[0];
        Utils.cloneTest(p2p, p2pClone);

        PhysicsRigidBody body8Clone
                = (PhysicsRigidBody) p2pClone.findOtherBody(body7Clone);
        Utils.cloneTest(body8, body8Clone);
        Assert.assertTrue(body8Clone.isDynamic());
        Assert.assertEquals(8f, body8Clone.getMass(), 0f);
        Assert.assertEquals(1, body8Clone.countIgnored());
        verifyParameters(body8Clone, 0.3f);

        // 2 joined bodies that don't ignore each another
        PhysicsRigidBody body5 = new PhysicsRigidBody(shape, 5f);
        setParameters(body5, 0.6f);
        PhysicsRigidBody body6 = new PhysicsRigidBody(shape, 6f);
        setParameters(body6, 0f);
        PhysicsJoint six = New6Dof.newInstance(body5, body6, new Vector3f(),
                new Quaternion(), RotationOrder.XYZ);

        PhysicsRigidBody body5Clone = Heart.deepCopy(body5);
        Utils.cloneTest(body5, body5Clone);
        Assert.assertTrue(body5Clone.isDynamic());
        Assert.assertEquals(5f, body5Clone.getMass(), 0f);
        Assert.assertEquals(0, body5Clone.countIgnored());
        verifyParameters(body5Clone, 0.6f);

        PhysicsJoint[] joints5Clone = body5Clone.listJoints();
        Assert.assertEquals(1, joints5Clone.length);
        PhysicsJoint sixClone = joints5Clone[0];
        Utils.cloneTest(six, sixClone);

        PhysicsRigidBody body6Clone
                = (PhysicsRigidBody) sixClone.findOtherBody(body5Clone);
        Utils.cloneTest(body6, body6Clone);
        Assert.assertTrue(body6Clone.isDynamic());
        Assert.assertEquals(6f, body6Clone.getMass(), 0f);
        Assert.assertEquals(0, body6Clone.countIgnored());
        verifyParameters(body6Clone, 0f);

        // 2 bodies that ignore each another but are not joined
        PhysicsRigidBody body3 = new PhysicsRigidBody(shape, 3f);
        setParameters(body3, 0.3f);
        PhysicsRigidBody body4 = new PhysicsRigidBody(shape, 4f);
        setParameters(body4, 0.6f);
        body3.addToIgnoreList(body4);

        PhysicsRigidBody body3Clone = Heart.deepCopy(body3);
        Utils.cloneTest(body3, body3Clone);
        Assert.assertTrue(body3Clone.isDynamic());
        Assert.assertEquals(3f, body3Clone.getMass(), 0f);
        Assert.assertEquals(1, body3Clone.countIgnored());
        verifyParameters(body3Clone, 0.3f);

        PhysicsCollisionObject[] ignoresClone = body3Clone.listIgnoredPcos();
        PhysicsRigidBody body4Clone = (PhysicsRigidBody) ignoresClone[0];
        Utils.cloneTest(body4, body4Clone);
        Assert.assertTrue(body4Clone.isDynamic());
        Assert.assertEquals(4f, body4Clone.getMass(), 0f);
        Assert.assertEquals(1, body4Clone.countIgnored());
        verifyParameters(body4Clone, 0.6f);
    }

    /**
     * Clone soft bodies.
     */
    private static void clonePsb() {
        // empty
        PhysicsSoftBody soft = new PhysicsSoftBody();
        setParameters(soft, 0f);
        verifyParameters(soft, 0f);
        PhysicsSoftBody softClone = Heart.deepCopy(soft);
        cloneTest(soft, softClone);

        // non-empty
        PhysicsSoftBody soft2 = new PhysicsSoftBody();
        NativeSoftBodyUtil.appendFromLineMesh(wireBox, soft2);
        setParameters(soft2, 0f);
        verifyParameters(soft2, 0f);
        PhysicsSoftBody soft2Clone = Heart.deepCopy(soft2);
        cloneTest(soft2, soft2Clone);

        clonePsbPair();
    }

    /**
     * Clone connected pairs of soft bodies.
     */
    private static void clonePsbPair() {
        // 2 bodies that ignore each another but are not joined
        PhysicsSoftBody soft3 = new PhysicsSoftBody();
        NativeSoftBodyUtil.appendFromLineMesh(wireBox, soft3);
        setParameters(soft3, 0.3f);
        PhysicsSoftBody soft4 = new PhysicsSoftBody();
        NativeSoftBodyUtil.appendFromLineMesh(wireBox, soft4);
        setParameters(soft4, 0.6f);
        soft3.addToIgnoreList(soft4);

        PhysicsSoftBody soft3Clone = Heart.deepCopy(soft3);
        Utils.cloneTest(soft3, soft3Clone);
        Assert.assertEquals(1, soft3Clone.countIgnored());
        verifyParameters(soft3Clone, 0.3f);

        PhysicsCollisionObject[] ignoresClone = soft3Clone.listIgnoredPcos();
        PhysicsSoftBody soft4Clone = (PhysicsSoftBody) ignoresClone[0];
        Utils.cloneTest(soft4, soft4Clone);
        Assert.assertEquals(1, soft4Clone.countIgnored());
        verifyParameters(soft4Clone, 0.6f);
    }

    /**
     * Clone vehicles.
     */
    private static void clonePv() {
        // TODO add wheel(s)
        PhysicsVehicle vehicle = new PhysicsVehicle(shape, 1f);
        setParameters(vehicle, 0f);
        verifyParameters(vehicle, 0f);
        PhysicsVehicle vehicleClone = Heart.deepCopy(vehicle);
        cloneTest(vehicle, vehicleClone);
    }

    /**
     * Clone rigid-body controls.
     */
    private static void cloneRbc() {
        // static
        RigidBodyControl rbc0 = new RigidBodyControl(shape, 0f);
        setParameters(rbc0, 0f);
        verifyParameters(rbc0, 0f);
        RigidBodyControl rbc0Clone = Heart.deepCopy(rbc0);
        Assert.assertTrue(rbc0Clone.isStatic());
        cloneTest(rbc0, rbc0Clone);

        // kinematic with mass=0
        RigidBodyControl k0 = new RigidBodyControl(shape, 0f);
        k0.setKinematic(true);
        setParameters(k0, 0f);
        verifyParameters(k0, 0f);
        RigidBodyControl k0Clone = Heart.deepCopy(k0);
        Assert.assertTrue(k0Clone.isKinematic());
        Assert.assertEquals(0f, k0Clone.getMass(), 0f);
        cloneTest(k0, k0Clone);

        // dynamic with mass=1
        RigidBodyControl rbc = new RigidBodyControl(shape, 1f);
        setParameters(rbc, 0f);
        verifyParameters(rbc, 0f);
        RigidBodyControl rbcClone = Heart.deepCopy(rbc);
        Assert.assertTrue(rbcClone.isDynamic());
        Assert.assertEquals(1f, rbcClone.getMass(), 0f);
        cloneTest(rbc, rbcClone);

        // kinematic with mass=4
        RigidBodyControl rbc4 = new RigidBodyControl(shape, 4f);
        rbc4.setKinematic(true);
        setParameters(rbc4, 0f);
        verifyParameters(rbc4, 0f);
        RigidBodyControl rbc4Clone = Heart.deepCopy(rbc4);
        Assert.assertTrue(rbc4Clone.isKinematic());
        Assert.assertEquals(4f, rbc4Clone.getMass(), 0f);
        cloneTest(rbc4, rbc4Clone);
    }

    /**
     * Clone soft-body controls.
     */
    private static void cloneSbc() {
        boolean localPhysics = false;
        boolean updateNormals = true;
        boolean mergeVertices = true;
        SoftBodyControl sbc = new SoftBodyControl(
                localPhysics, updateNormals, mergeVertices);
        Geometry sbcGeom = new Geometry("sbcGeom", wireBox);
        sbcGeom.addControl(sbc);
        PhysicsSoftBody soft3 = sbc.getBody();
        setParameters(soft3, 0f);
        verifyParameters(soft3, 0f);
        Geometry sbcGeomClone = Heart.deepCopy(sbcGeom);
        SoftBodyControl sbcClone = (SoftBodyControl) sbcGeomClone.getControl(0);
        PhysicsSoftBody soft3Clone = sbcClone.getBody();
        cloneTest(soft3, soft3Clone);
    }

    private static void cloneTest(PhysicsBody body, PhysicsBody bodyClone) {
        Utils.cloneTest(body, bodyClone);

        if (body instanceof PhysicsRigidBody) {
            PhysicsRigidBody rBody = (PhysicsRigidBody) body;
            PhysicsRigidBody rBodyClone = (PhysicsRigidBody) bodyClone;
            Utils.cloneTest(
                    rBody.getMotionState(), rBodyClone.getMotionState());

        } else if (body instanceof PhysicsSoftBody) {
            PhysicsSoftBody sBody = (PhysicsSoftBody) body;
            PhysicsSoftBody sBodyClone = (PhysicsSoftBody) bodyClone;
            Assert.assertNotSame(
                    sBodyClone.getSoftConfig(), sBody.getSoftConfig());
            Utils.cloneTest(
                    sBody.getSoftMaterial(), sBodyClone.getSoftMaterial());
            Utils.cloneTest(sBody.getWorldInfo(), sBodyClone.getWorldInfo());
        }

        // Verify that cloning didn't affect the original.
        verifyParameters(body, 0f);

        // Verify that the clone matches the original.
        verifyParameters(bodyClone, 0f);

        // Verify that changing the original doesn't affect the clone.
        setParameters(body, 0.3f);
        verifyParameters(body, 0.3f);
        verifyParameters(bodyClone, 0f);

        // Verify that changing the clone doesn't affect the original.
        setParameters(bodyClone, 0.6f);
        verifyParameters(body, 0.3f);
        verifyParameters(bodyClone, 0.6f);

        PhysicsBody bodyCopy = BinaryExporter.saveAndLoad(assetManager, body);
        verifyParameters(bodyCopy, 0.3f);

        PhysicsBody bodyCloneCopy
                = BinaryExporter.saveAndLoad(assetManager, bodyClone);
        verifyParameters(bodyCloneCopy, 0.6f);

        PhysicsBody xmlCopy = Utils.saveAndLoadXml(assetManager, body);
        verifyParameters(xmlCopy, 0.3f);

        if (body instanceof PhysicsRigidBody) {
            PhysicsRigidBody rBody = (PhysicsRigidBody) body;

            // Test rebuild while not added to a physics space.
            rBody.rebuildRigidBody();
            setRigidInertia(rBody, 0.3f);
            verifyParameters(rBody, 0.3f);

            // Add to the physics space.
            space.addCollisionObject(body);
            setParameters(body, 0f);
            verifyParameters(body, 0f);

            // Test save-and-load while added to a physics space.
            PhysicsBody copy2 = BinaryExporter.saveAndLoad(assetManager, body);
            verifyParameters(copy2, 0f);

            // Test rebuild while added to a physics space.
            rBody.rebuildRigidBody();
            setRigidInertia(rBody, 0f);
            verifyParameters(rBody, 0f);

            space.removeCollisionObject(body);
        }
    }

    /**
     * Clone vehicle controls.
     */
    private static void cloneVc() {
        // TODO add wheel(s)
        VehicleControl vc = new VehicleControl(shape, 1f);
        setParameters(vc, 0f);
        verifyParameters(vc, 0f);
        VehicleControl vcClone = Heart.deepCopy(vc);
        cloneTest(vc, vcClone);
    }

    private static void setParameters(PhysicsBody pco, float b) {
        pco.setGravity(new Vector3f(b + 0.031f, b + 0.032f, b + 0.033f));

        if (pco instanceof PhysicsRigidBody) {
            setRigid((PhysicsRigidBody) pco, b);
        } else if (pco instanceof PhysicsSoftBody) {
            setSoft((PhysicsSoftBody) pco, b);
        } else {
            throw new IllegalArgumentException(pco.getClass().getName());
        }
    }

    private static void setRigid(PhysicsRigidBody body, float b) {
        boolean flag = (b > 0.15f && b < 0.45f);
        body.setContactResponse(flag);
        body.setProtectGravity(!flag);

        int afMode = Math.round(b / 0.3f);
        body.setAnisotropicFriction(
                new Vector3f(b + 0.004f, b + 0.005f, b + 0.006f), afMode);

        body.setAngularDamping(b + 0.01f);
        Vector3f aFactor = new Vector3f(b + 0.02f, b + 0.021f, b + 0.022f);
        body.setAngularFactor(aFactor);
        body.setSleepingThresholds(b + 0.17f, b + 0.03f);
        body.setAngularVelocity(new Vector3f(b + 0.04f, b + 0.05f, b + 0.06f));
        body.setCcdMotionThreshold(b + 0.07f);
        body.setCcdSweptSphereRadius(b + 0.08f);
        body.setContactDamping(b + 0.084f);
        body.setContactProcessingThreshold(b + 0.0845f);
        body.setContactStiffness(b + 0.085f);
        body.setFriction(b + 0.09f);
        setRigidInertia(body, b);
        body.setLinearDamping(b + 0.13f);
        Vector3f lFactor = new Vector3f(b + 0.14f, b + 0.15f, b + 0.16f);
        body.setLinearFactor(lFactor);
        body.setPhysicsLocation(new Vector3f(b + 0.18f, b + 0.19f, b + 0.20f));

        Quaternion orient
                = new Quaternion(b + 0.21f, b + 0.22f, b + 0.23f, b + 0.24f);
        MyQuaternion.normalizeLocal(orient);
        Matrix3f matrix = orient.toRotationMatrix();
        body.setPhysicsRotation(matrix);

        body.setRestitution(b + 0.25f);
        body.setRollingFriction(b + 0.254f);
        body.setSpinningFriction(b + 0.255f);

        // Linear velocity affects deactivation time, so set it first!
        body.clearForces();
        Vector3f force = new Vector3f(b + 0.231f, b + 0.232f, b + 0.233f);
        body.applyCentralForce(force.divide(lFactor));
        Vector3f torque = new Vector3f(b + 0.241f, b + 0.242f, b + 0.243f);
        body.applyTorque(torque.divide(aFactor));
        body.setLinearVelocity(new Vector3f(b + 0.26f, b + 0.27f, b + 0.28f));
        body.setDeactivationTime(b + 0.087f);

        if (body instanceof PhysicsVehicle) {
            // TODO additional setup
        }
    }

    private static void setRigidInertia(PhysicsRigidBody body, float b) {
        body.setInverseInertiaLocal(
                new Vector3f(b + 0.122f, b + 0.123f, b + 0.124f));
    }

    private static void setSoft(PhysicsSoftBody body, float b) {
        boolean flag = (b > 0.15f && b < 0.45f);
        int n = Math.round(10f * b);

        body.setProtectWorldInfo(!flag);

        SoftBodyConfig config = body.getSoftConfig();
        for (Sbcp sbcp : Sbcp.values()) {
            float value = b + 0.001f * sbcp.ordinal();
            config.set(sbcp, value);
        }

        SoftBodyWorldInfo info = body.getWorldInfo();
        info.setAirDensity(b + 0.03f);
        info.setMaxDisplacement(b + 0.034f);
        info.setWaterDensity(b + 0.035f);
        info.setWaterOffset(b + 0.036f);

        Vector3f normal = new Vector3f(b + 0.1f, b + 0.2f, b + 0.3f);
        normal.normalizeLocal();
        info.setWaterNormal(normal);

        config.setClusterIterations(n);
        config.setDriftIterations(n + 1);
        config.setPositionIterations(n + 2);
        config.setVelocityIterations(n + 3);

        int flags;
        if (flag) {
            flags = ConfigFlag.CL_RS | ConfigFlag.CL_SS | ConfigFlag.CL_SELF;
        } else {
            flags = ConfigFlag.SDF_RS | ConfigFlag.VF_SS;
        }
        config.setCollisionFlags(flags);

        SoftBodyMaterial material = body.getSoftMaterial();
        material.setAngularStiffness(b + 0.04f);
        material.setLinearStiffness(b + 0.041f);
        material.setVolumeStiffness(b + 0.042f);
    }

    private static void verifyParameters(PhysicsBody pco, float b) {
        Assert.assertNotNull(pco);
        Utils.assertEquals(
                b + 0.031f, b + 0.032f, b + 0.033f, pco.getGravity(null), 0f);

        if (pco instanceof PhysicsRigidBody) {
            verifyRigid((PhysicsRigidBody) pco, b);
        } else if (pco instanceof PhysicsSoftBody) {
            verifySoft((PhysicsSoftBody) pco, b);
        } else {
            throw new IllegalArgumentException(pco.getClass().getName());
        }
    }

    private static void verifyRigid(PhysicsRigidBody body, float b) {
        boolean flag = (b > 0.15f && b < 0.45f);
        Assert.assertEquals(flag, body.isContactResponse());
        Assert.assertEquals(!flag, body.isGravityProtected());

        int index = Math.round(b / 0.3f);
        if (index == 0) {
            Assert.assertFalse(body.hasAnisotropicFriction(AfMode.either));
        } else {
            Assert.assertTrue(body.hasAnisotropicFriction(index));
            Vector3f c = body.getAnisotropicFriction(null);
            Utils.assertEquals(b + 0.004f, b + 0.005f, b + 0.006f, c, 0f);
        }

        Assert.assertEquals(b + 0.01f, body.getAngularDamping(), 0f);

        Vector3f af = body.getAngularFactor(null);
        Utils.assertEquals(b + 0.02f, b + 0.021f, b + 0.022f, af, 0f);

        Assert.assertEquals(b + 0.03f, body.getAngularSleepingThreshold(), 0f);
        Assert.assertEquals(b + 0.07f, body.getCcdMotionThreshold(), 0f);
        Assert.assertEquals(b + 0.08f, body.getCcdSweptSphereRadius(), 0f);
        Assert.assertEquals(b + 0.084f, body.getContactDamping(), 0f);
        Assert.assertEquals(
                b + 0.0845f, body.getContactProcessingThreshold(), 0f);
        Assert.assertEquals(b + 0.085f, body.getContactStiffness(), 0f);
        Assert.assertEquals(b + 0.087f, body.getDeactivationTime(), 0f);
        Assert.assertEquals(b + 0.09f, body.getFriction(), 0f);

        Vector3f i = body.getInverseInertiaLocal(null);
        Utils.assertEquals(b + 0.122f, b + 0.123f, b + 0.124f, i, 0f);

        Assert.assertEquals(b + 0.13f, body.getLinearDamping(), 0f);

        Vector3f f = body.getLinearFactor(null);
        Utils.assertEquals(b + 0.14f, b + 0.15f, b + 0.16f, f, 0f);

        Assert.assertEquals(b + 0.17f, body.getLinearSleepingThreshold(), 0f);

        Vector3f x = body.getPhysicsLocation(null);
        Utils.assertEquals(b + 0.18f, b + 0.19f, b + 0.20f, x, 0f);

        Quaternion orient
                = new Quaternion(b + 0.21f, b + 0.22f, b + 0.23f, b + 0.24f);
        MyQuaternion.normalizeLocal(orient);
        Matrix3f matrix = orient.toRotationMatrix();
        Matrix3f m = body.getPhysicsRotationMatrix(null);
        Assert.assertEquals(matrix, m);

        Assert.assertEquals(b + 0.25f, body.getRestitution(), 0f);
        Assert.assertEquals(b + 0.254f, body.getRollingFriction(), 0f);
        Assert.assertEquals(b + 0.255f, body.getSpinningFriction(), 0f);

        Utils.assertEquals(b + 0.231f, b + 0.232f, b + 0.233f,
                body.totalAppliedForce(null), 1e-6f);
        Utils.assertEquals(b + 0.241f, b + 0.242f, b + 0.243f,
                body.totalAppliedTorque(null), 1e-6f);

        if (body.isDynamic()) {
            Vector3f w = body.getAngularVelocity(null);
            Utils.assertEquals(b + 0.04f, b + 0.05f, b + 0.06f, w, 0f);

            Vector3f v = body.getLinearVelocity(null);
            Utils.assertEquals(b + 0.26f, b + 0.27f, b + 0.28f, v, 0f);
        }

        if (body instanceof PhysicsVehicle) {
            // TODO
        }
    }

    private static void verifySoft(PhysicsSoftBody body, float b) {
        boolean flag = (b > 0.15f && b < 0.45f);
        int n = Math.round(10f * b);

        Assert.assertEquals(!flag, body.isWorldInfoProtected());

        SoftBodyConfig config = body.getSoftConfig();
        for (Sbcp sbcp : Sbcp.values()) {
            float expected = b + 0.001f * sbcp.ordinal();
            float actual = config.get(sbcp);
            Assert.assertEquals(expected, actual, 1e-6f);
        }

        SoftBodyWorldInfo info = body.getWorldInfo();
        Assert.assertEquals(b + 0.03f, info.airDensity(), 0f);
        Utils.assertEquals(
                b + 0.031f, b + 0.032f, b + 0.033f, info.copyGravity(null), 0f);
        Assert.assertEquals(b + 0.034f, info.maxDisplacement(), 0f);
        Assert.assertEquals(b + 0.035f, info.waterDensity(), 0f);
        Assert.assertEquals(b + 0.036f, info.waterOffset(), 0f);

        Vector3f normal = new Vector3f(b + 0.1f, b + 0.2f, b + 0.3f);
        normal.normalizeLocal();
        Utils.assertEquals(normal, info.copyWaterNormal(null), 1e-5f);

        Assert.assertEquals(n, config.clusterIterations());
        Assert.assertEquals(n + 1, config.driftIterations());
        Assert.assertEquals(n + 2, config.positionIterations());
        Assert.assertEquals(n + 3, config.velocityIterations());

        int flags;
        if (flag) {
            flags = ConfigFlag.CL_RS | ConfigFlag.CL_SS | ConfigFlag.CL_SELF;
        } else {
            flags = ConfigFlag.SDF_RS | ConfigFlag.VF_SS;
        }
        Assert.assertEquals(flags, config.collisionFlags());

        SoftBodyMaterial material = body.getSoftMaterial();
        Assert.assertEquals(b + 0.04f, material.angularStiffness(), 0f);
        Assert.assertEquals(b + 0.041f, material.linearStiffness(), 0f);
        Assert.assertEquals(b + 0.042f, material.volumeStiffness(), 0f);
    }
}
