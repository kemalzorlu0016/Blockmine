const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_123";
const BOB_UID = "bob_456";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

function validProfile(uid) {
  const now = new Date();
  return {
    userId: uid,
    username: "BlockMaster",
    titleBadge: "Usta Mimar",
    skinPreset: "diamond_knight",
    hairColorHex: "#3B271A",
    tunicColorHex: "#00B4D8",
    pantsColorHex: "#1D3557",
    capeStyle: "royal_gold",
    level: 5,
    xp: 1250,
    coins: 600,
    gems: 45,
    dailyStreak: 3,
    lastClaimDay: 19675,
    blocksPlaced: 120,
    blocksMined: 85,
    templatesBuilt: 4,
    languageCode: "tr",
    createdAt: now,
    updatedAt: now,
  };
}

test("Unauthenticated user: cannot read or create profiles", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).set(validProfile(ALICE_UID)));
});

test("Authenticated user: can create and read own profile, but cannot read another user's profile", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).set(validProfile(ALICE_UID)));
  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).get());

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertFails(bobDb.collection("users").doc(ALICE_UID).get());
});

test("Shadow update test: rejects ghost fields on user profile update", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).set(validProfile(ALICE_UID)));

  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      coins: 9999,
      isAdmin: true,
      updatedAt: new Date(),
    })
  );
});

test("Friends subcollection: owner can create and list friends; non-owner rejected", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const now = new Date();
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("friends").doc("friend_1").set({
      userId: ALICE_UID,
      friendName: "KaanBuilder",
      friendTag: "#TR42",
      status: "online",
      serverRoom: "Anadolu Survival #1",
      skinPreset: "emerald_explorer",
      createdAt: now,
      updatedAt: now,
    })
  );
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("friends").get()
  );

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("friends").get()
  );
});

test("Servers collection: member can create and query with array-contains memberUids", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const now = new Date();
  await assertSucceeds(
    aliceDb.collection("servers").doc("room_1").set({
      ownerId: ALICE_UID,
      roomName: "Anadolu Survival #1",
      gameMode: "creative",
      region: "EU-TR",
      playerCount: 3,
      maxPlayers: 16,
      memberUids: [ALICE_UID],
      lastChatMessage: "Merhaba BlockMine!",
      createdAt: now,
      updatedAt: now,
    })
  );

  await assertSucceeds(
    aliceDb.collection("servers").where("memberUids", "array-contains", ALICE_UID).get()
  );

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertFails(
    bobDb.collection("servers").doc("room_1").get()
  );
});
