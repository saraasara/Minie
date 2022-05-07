/*
 Copyright (c) 2021-2022, Stephen Gold
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
package jme3utilities.minie.test.issue;

import com.jme3.app.SimpleApplication;
import com.jme3.bullet.BulletAppState;
import com.jme3.bullet.PhysicsSpace;
import com.jme3.bullet.PhysicsTickListener;
import com.jme3.bullet.collision.shapes.CollisionShape;
import com.jme3.bullet.collision.shapes.GImpactCollisionShape;
import com.jme3.bullet.control.BetterCharacterControl;
import com.jme3.bullet.control.RigidBodyControl;
import com.jme3.bullet.objects.PhysicsBody;
import com.jme3.bullet.objects.PhysicsRigidBody;
import com.jme3.math.FastMath;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.shape.Quad;

/**
 * Test for Minie issue #18 (BetterCharacterController hops across seams) using
 * a GImpactCollisionShape. If the issue is present, numeric data will be
 * printed to {@code System.out}.
 *
 * @author Stephen Gold sgold@sonic.net
 */
public class TestIssue18Gimpact
        extends SimpleApplication
        implements PhysicsTickListener {
    // *************************************************************************
    // fields

    /**
     * control under test
     */
    private BetterCharacterControl bcc;
    /**
     * true if character will move toward +X, false if it will move toward -X
     */
    private boolean increasingX;
    /**
     * largest Y value seen so far: anything larger than 0.05 is an issue
     */
    private float maxElevation = 0.05f;
    /**
     * count of physics timesteps simulated
     */
    private int tickCount = 0;
    // *************************************************************************
    // new methods exposed

    public static void main(String[] ignored) {
        TestIssue18Gimpact application = new TestIssue18Gimpact();
        application.start();
    }
    // *************************************************************************
    // SimpleApplication methods

    @Override
    public void simpleInitApp() {
        PhysicsSpace physicsSpace = configurePhysics();
        addGround(physicsSpace);

        Node controlledNode = new Node("controlled node");
        rootNode.attachChild(controlledNode);

        float characterRadius = 1f;
        float characterHeight = 4f;
        float characterMass = 1f;
        bcc = new BetterCharacterControl(characterRadius, characterHeight,
                characterMass);
        controlledNode.addControl(bcc);
        physicsSpace.add(bcc);
    }

    @Override
    public void simpleUpdate(float tpf) {
        /*
         * Terminate the test after 200 time steps.
         */
        if (tickCount > 200) {
            stop();
        }
    }
    // *************************************************************************
    // PhysicsTickListener methods

    @Override
    public void physicsTick(PhysicsSpace space, float timeStep) {
        /*
         * Determine the character's elevation and print it if it's a new high.
         */
        PhysicsRigidBody body = bcc.getRigidBody();
        Vector3f location = body.getPhysicsLocation();
        if (location.y > maxElevation) {
            maxElevation = location.y;
            System.out.println(tickCount + ": " + location);
        }
    }

    @Override
    public void prePhysicsTick(PhysicsSpace space, float timeStep) {
        ++tickCount;
        /*
         * Walk rapidly back and forth across the seam between the 2 triangles.
         */
        Vector3f desiredVelocity = new Vector3f();
        float walkSpeed = 99f;
        if (increasingX) {
            desiredVelocity.x = walkSpeed;
        } else {
            desiredVelocity.x = -walkSpeed;
        }

        Vector3f location = bcc.getRigidBody().getPhysicsLocation();
        if (increasingX && location.x > 7f) {
            // stop and reverse direction
            desiredVelocity.zero();
            increasingX = false;
        } else if (!increasingX && location.x < -7f) {
            // stop and reverse direction
            desiredVelocity.zero();
            increasingX = true;
        }

        bcc.setWalkDirection(desiredVelocity);
    }
    // *************************************************************************
    // private methods

    /**
     * Add a ground body to the specified PhysicsSpace.
     *
     * @param physicsSpace (not null)
     */
    private void addGround(PhysicsSpace physicsSpace) {
        Mesh quad = new Quad(1000f, 1000f);
        Spatial ground = new Geometry("ground", quad);
        ground.move(-500f, 0f, 500f);
        ground.rotate(-FastMath.HALF_PI, 0f, 0f);

        CollisionShape shape = new GImpactCollisionShape(quad);
        RigidBodyControl rbc
                = new RigidBodyControl(shape, PhysicsBody.massForStatic);
        rbc.setPhysicsSpace(physicsSpace);
        ground.addControl(rbc);
    }

    /**
     * Configure physics during startup.
     */
    private PhysicsSpace configurePhysics() {
        BulletAppState bulletAppState = new BulletAppState();
        stateManager.attach(bulletAppState);

        PhysicsSpace result = bulletAppState.getPhysicsSpace();
        result.addTickListener(this);

        return result;
    }
}
