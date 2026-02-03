# SDK 2 Test Data

This folder contains mock data for testing EFX SDK version 2 functionality.

## Node Hierarchy

```
ND-Root (non-rep)
|
+-- ND-SubNode (non-rep)
|   +-- ND-SubSubNode (non-rep)          <- non-rep in non-rep
|   +-- ND-SubSubNode2 (non-rep)         <- non-rep in non-rep (sibling)
|   +-- ND-RepeatableInSubNode (REP)     <- rep in non-rep
|   +-- ND-RepeatableInSubNode2 (REP)    <- rep in non-rep (sibling)
|
+-- ND-RepeatableNode (REP)
    +-- ND-NonRepeatableSubNode (non-rep)       <- non-rep in rep
    |   +-- ND-RepeatableSubSubNode (REP)       <- rep in non-rep in rep
    +-- ND-NonRepeatableSubNode2 (non-rep)      <- non-rep in rep (sibling)
    +-- ND-RepeatableInRepeatableNode (REP)     <- rep in rep
    +-- ND-RepeatableInRepeatableNode2 (REP)    <- rep in rep (sibling)
```

## Repeatability Coverage

The structure covers all 4 parent/child repeatability combinations:

| Parent Type | Child Type | Nodes |
|-------------|------------|-------|
| Non-rep | Non-rep | ND-SubSubNode, ND-SubSubNode2 |
| Non-rep | Rep | ND-RepeatableInSubNode, ND-RepeatableInSubNode2 |
| Rep | Non-rep | ND-NonRepeatableSubNode, ND-NonRepeatableSubNode2 |
| Rep | Rep | ND-RepeatableInRepeatableNode, ND-RepeatableInRepeatableNode2 |

Sibling pairs enable cross-branch backtracking tests.

## Field Naming Convention

Fields use the pattern `BT-XY-Type` where:
- **X** = Branch (1 = ND-SubNode branch, 2 = ND-RepeatableNode branch)
- **Y** = Node within branch (1-4 for children of each branch, 5 for deeper nesting)
- **Type** = Data type (Text, TextMultilingual, Indicator, Number, Date, Time, Measure)

| Prefix | Parent Node | Repeatability from Root |
|--------|-------------|------------------------|
| BT-00-* | ND-Root | scalar |
| BT-11-* | ND-SubSubNode | scalar |
| BT-12-* | ND-SubSubNode2 | scalar |
| BT-13-* | ND-RepeatableInSubNode | SEQUENCE (self-rep) |
| BT-14-* | ND-RepeatableInSubNode2 | SEQUENCE (self-rep) |
| BT-21-* | ND-NonRepeatableSubNode | SEQUENCE (ancestor-rep) |
| BT-22-* | ND-NonRepeatableSubNode2 | SEQUENCE (ancestor-rep) |
| BT-23-* | ND-RepeatableInRepeatableNode | SEQUENCE (self + ancestor) |
| BT-24-* | ND-RepeatableInRepeatableNode2 | SEQUENCE (self + ancestor) |
| BT-25-* | ND-RepeatableSubSubNode | SEQUENCE (self + ancestor) |

## Files

- `fields-sdk2.json` - Field definitions with XPath expressions
- `nodes-sdk2.json` - Node definitions with hierarchy and repeatability

## Test Scenarios Enabled

1. **Scalar vs SEQUENCE resolution** - Fields in non-rep vs rep contexts
2. **Ancestor repeatability** - Fields under repeatable ancestors
3. **Cross-branch navigation** - Paths that backtrack through parent nodes
4. **Multiple data types** - Each node has fields of 7 different types
