/*
 * Copyright 2011 SecondMarket Labs, LLC.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at  http://www.apache.org/licenses/LICENSE-2.0
 *  
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and limitations under the License.
 */

package org.mongeez;

import com.mongodb.DB;
import com.mongodb.Mongo;
import org.springframework.core.io.ClassPathResource;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.fail;

@Test
public class MongeezTest {
    private String dbName = "test_mongeez";
    private Mongo mongo;
    private DB db;

    @BeforeMethod
    protected void setUp() throws Exception {
        mongo = new Mongo();
        db = mongo.getDB(dbName);

        db.dropDatabase();
    }

    private Mongeez create(String path) {
        Mongeez mongeez = new Mongeez();
        mongeez.setFile(new ClassPathResource(path));
        mongeez.setMongo(mongo);
        mongeez.setDbName(dbName);
        return mongeez;
    }

    @Test(groups = "dao")
    public void testMongeez() throws Exception {
        Mongeez mongeez = create("mongeez.xml");

        mongeez.process();

        assertEquals(db.getCollection("mongeez").count(), 5);

        assertEquals(db.getCollection("organization").count(), 2);
        assertEquals(db.getCollection("user").count(), 2);
    }

    @Test(groups = "dao")
    public void testRunTwice() throws Exception {
        testMongeez();
        testMongeez();
    }

    @Test(groups = "dao")
    public void testFailOnError_False() throws Exception {
        assertEquals(db.getCollection("mongeez").count(), 0);

        Mongeez mongeez = create("mongeez_fail.xml");
        mongeez.process();

        assertEquals(db.getCollection("mongeez").count(), 2);
    }

    @Test(groups = "dao", expectedExceptions = com.mongodb.CommandFailureException.class)
    public void testFailOnError_True() throws Exception {
        Mongeez mongeez = create("mongeez_fail_fail.xml");
        mongeez.process();
    }

    @Test(groups = "dao")
    public void testNoFiles() throws Exception {
        Mongeez mongeez = create("mongeez_empty.xml");
        mongeez.process();

        assertEquals(db.getCollection("mongeez").count(), 1);
    }

    @Test(groups = "dao")
    public void testNoFailureOnEmptyChangeLog() throws Exception {
        assertEquals(db.getCollection("mongeez").count(), 0);

        Mongeez mongeez = create("mongeez_empty_changelog.xml");
        mongeez.process();

        assertEquals(db.getCollection("mongeez").count(), 1);
    }

    @Test(groups = "dao")
    public void testFailureOnNoChangeFilesBlock() throws Exception {
        assertEquals(db.getCollection("mongeez").count(), 0);

        Mongeez mongeez = create("mongeez_no_changefiles_declared.xml");

        try
        {
            mongeez.process();
        }
        catch (MongeezException e)
        {
            assertEquals(e.getMessage(), "org.mongeez.MongeezException: The file \"mongeez_no_changefiles_declared.xml\" doesn't seem to contain a changeFiles declaration. Are you using the correct file to initialize Mongeez?");
            return;
        }

        fail("Expected MongeezException did not occur");
    }

    @Test(groups = "dao")
    public void testChangesWContextContextNotSet() throws Exception {
        assertEquals(db.getCollection("mongeez").count(), 0);

        Mongeez mongeez = create("mongeez_contexts.xml");
        mongeez.process();
        assertEquals(db.getCollection("mongeez").count(), 2);
        assertEquals(db.getCollection("car").count(), 2);
        assertEquals(db.getCollection("user").count(), 0);
        assertEquals(db.getCollection("organization").count(), 0);
        assertEquals(db.getCollection("house").count(), 0);
    }

    @Test(groups = "dao")
    public void testChangesWContextContextSetToUsers() throws Exception {
        assertEquals(db.getCollection("mongeez").count(), 0);

        Mongeez mongeez = create("mongeez_contexts.xml");
        mongeez.setContext("users");
        mongeez.process();
        assertEquals(db.getCollection("mongeez").count(), 4);
        assertEquals(db.getCollection("car").count(), 2);
        assertEquals(db.getCollection("user").count(), 2);
        assertEquals(db.getCollection("organization").count(), 0);
        assertEquals(db.getCollection("house").count(), 2);
    }

    @Test(groups = "dao")
    public void testChangesWContextContextSetToOrganizations() throws Exception {
        assertEquals(db.getCollection("mongeez").count(), 0);

        Mongeez mongeez = create("mongeez_contexts.xml");
        mongeez.setContext("organizations");
        mongeez.process();
        assertEquals(db.getCollection("mongeez").count(), 4);
        assertEquals(db.getCollection("car").count(), 2);
        assertEquals(db.getCollection("user").count(), 0);
        assertEquals(db.getCollection("organization").count(), 2);
        assertEquals(db.getCollection("house").count(), 2);
    }
}
