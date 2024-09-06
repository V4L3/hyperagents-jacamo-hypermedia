/* Initial beliefs and rules */


/* Initial goals */

!start.

+!start : true <-
  .print("hello world. I am the S-Agent");
  makeArtifact("notification-server", "org.hyperagents.jacamo.artifacts.yggdrasil.NotificationServerArtifact", ["localhost", 8083], _);
  start;
  makeArtifact("vector-clock", "org.hyperagents.jacamo.artifacts.utils.time.VectorClockArtifact", [], _);
  !explore;
  .print("Environment explored...").
  

// When a new obsProperty update is received we check if it is out of order and mark it as such
+isLocked(X)[vectorClock(VClock),artifact_name(ArtName),roomName(RoomName),toBeChecked("true")] : true <-
    isOutOfOrder(VClock, Result);
    .println("Received event with vector clock: ", VClock, ", this event is out of order: ", Result);
    -+isLocked(X)[artifact_name(ArtName),isOutOfOrder(Result),roomName(RoomName)];
    .

+fallDetected(X)[vectorClock(VClock),artifact_name(ArtName),roomName(RoomName),toBeChecked("true")] : true <-
    isOutOfOrder(VClock, Result);
    .println("Received event with vector clock: ", VClock, ", this event is out of order: ", Result);
    -+fallDetected(X)[artifact_name(ArtName),isOutOfOrder(Result),roomName(RoomName)];
    .

// Reevaluation plans
+!reevaluateFallDetection(RoomName) : isLocked("false")[roomName(RoomName)] & fallDetected("true")[roomName(RoomName)] <-
    .print("Reevaluated fall detection in room ", RoomName, ", there is a need for action");
    .print("Notifying according authorities for room ", RoomName, "...");
    .

+!reevaluateFallDetection[roomName(RoomName)] : true <-
    .print("Reevaluated fall detection, there is no need for action");
    .

// Handle the fall detection event
+fallDetected("true")[artifact_name(ArtName),roomName(RoomName)] : fallDetectionIsActive("true")[roomName(RoomName)] <-
    -fallDetected("true")[artifact_name(ArtName)];
    .print("Fall detected...");
    .print("Notifying according authorities...");
    .

+fallDetected("true")[artifact_name(ArtName),roomName(RoomName)] : true <-
    -fallDetected("true")[artifact_name(ArtName)];
    .print("Fall detected...");
    .print("Room is marked empty must be a false positive");
    +faslePositive(ArtName)[roomName(RoomName)];
    .

// Handle the lock/unlock events

// When an lock/unlock event is received out of order a reevaluation is triggered
+isLocked("false")[artifact_name(ArtName),isOutOfOrder(true),roomName(RoomName)] : true <-
    +isLocked("false")[artifact_name(ArtName)];
    .print("Received out of order event for a room where there is a false positive");
    !reevaluateFallDetection(RoomName);
    .

+isLocked("true")[artifact_name(ArtName),roomName(RoomName)] : true <-
    -isLocked("false")[artifact_name(ArtName)];
    .print("The room ", RoomName, " is LOCKED: ");
    -+fallDetectionIsActive("false")[roomName(RoomName)];
    .print("Fall detection for room ", ArtName ," is now INACTIVE");
    .

+isLocked("false")[artifact_name(ArtName),roomName(RoomName)] : true <-
    -isLocked("true")[artifact_name(ArtName)];
    .print("The Room ", ArtName, " is UNLOCKED: ");
    -+fallDetectionIsActive("true")[roomName(RoomName)];
    .print("Fall detection for room ", ArtName ," is now ACTIVE");
    .

// Handle artifact registration for WebSub
+relevant_artifact(ArtifactIRI, ArtifactName)[workspace(WorkspaceNameStr,WorkspaceIRI)] : true <-
  makeArtifact(ArtifactName, "org.hyperagents.jacamo.artifacts.wot.WebSubThingArtifact", [ArtifactIRI], ArtID);
  focus(ArtID);
  !registerForWebSub(ArtifactName, ArtID);
  registerArtifactForFocus(WorkspaceIRI, ArtifactIRI, ArtID, ArtifactName);
  .

+!registerForWebSub(ArtifactName, ArtID) : true <-
  ?websub(HubIRI, TopicIRI)[artifact_id(ArtID)];
  registerArtifactForWebSub(TopicIRI, ArtID, HubIRI);
  .

-!registerForWebSub(ArtifactName, ArtID) : true <-
  .print("WebSub not available for artifact: ", ArtifactName);
  .

{ include("inc/crawling.asl") }
{ include("$jacamoJar/templates/common-cartago.asl") }
{ include("$jacamoJar/templates/common-moise.asl") }

