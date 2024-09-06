+!explore:  home_workspace(HomeName, HomeWorkspaceIRI) & webid(MyWebID)
    <- .print("My home workspace IRI is: ", HomeWorkspaceIRI);
       +seen(HomeWorkspaceIRI, HomeName);
       makeArtifact(HomeName, "org.hyperagents.jacamo.artifacts.yggdrasil.WorkspaceThingArtifact", [HomeWorkspaceIRI], ArtId);
       focus(ArtId);
       setOperatorWebId(MyWebID)[artifact_id(ArtId)];
       .print("Created workspace artifact! Joining with WebID: ", MyWebID);
       joinHypermediaWorkspace[artifact_id(ArtId)];
       +joinedWsp(HomeWorkspaceIRI);
    .

+parentHypermediaWorkspace(ParentIRI, ParentName, _):  not seen(_, ParentName) & parentHypermediaWorkspace(ParentIRI,_,_)
    <- .print("Parent workspace already discovered: ", ParentName);
    .

+parentHypermediaWorkspace(ParentIRI, ParentName, _):  not seen(_, ParentName) & not parentHypermediaWorkspace(ParentIRI,_,_)
    <- makeArtifact(ParentName, "org.hyperagents.jacamo.artifacts.yggdrasil.WorkspaceThingArtifact", [ParentIRI], ArtId);
       focus(ArtId);
       +seen(ParentIRI, ParentName);
    .

+hypermediaWorkspace(WorkspaceIRI, WorkspaceName, _):  not seen(_, WorkspaceName)
    <- makeArtifact(WorkspaceName, "org.hyperagents.jacamo.artifacts.yggdrasil.WorkspaceThingArtifact", [WorkspaceIRI], ArtId);
       focus(ArtId);
       +seen(WorkspaceIRI, WorkspaceName);
    .

+hypermediaArtifact(ArtifactIRI, ArtifactName, SemanticTypes)[artifact_name(WorkspaceName),artifact_id(WorkspaceId)]
    : relevant_semantic_type(RelevantSemanticType) & .member(RelevantSemanticType, SemanticTypes)
    <- .print("Discovered relevant artifact: ", ArtifactName);
       .term2string(WorkspaceName, WorkspaceNameStr);
       ?seen(WorkspaceIRI, WorkspaceNameStr);
       +relevant_artifact(ArtifactIRI, ArtifactName)[workspace(WorkspaceNameStr,WorkspaceIRI)];
    .

+hypermediaArtifact(ArtifactIRI, ArtifactName, SemanticTypes)[artifact_name(WorkspaceName),artifact_id(WorkspaceId)]
    : .member("http://example.org/Transformer", SemanticTypes)
    <- .print("Discovered Transformer artifact: ", ArtifactName);
       .term2string(WorkspaceName, WorkspaceNameStr);
       ?seen(WorkspaceIRI, WorkspaceNameStr);
       +relevant_artifact(ArtifactIRI, ArtifactName)[workspace(WorkspaceNameStr,WorkspaceIRI)];
    .
   
+hypermediaArtifact: true <-
      .print("Discovered Irrelevant Hypermedia artifact.").

