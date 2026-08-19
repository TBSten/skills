# Processor design: adapting the example to your annotation

The layering rules themselves ship into the generated project as
`assets/rules/ksp-architecture.md` (and the three path-scoped companions) — that file is the single
source of truth for the dependency table. This file is the setup-time guide: how to turn the
`greeting` example into your own feature, and the design decisions behind it.

## Rename map

The example uses one annotation, `@Greeting`, threaded through every layer so the shape is visible
end to end:

| Example | Yours |
|---|---|
| `Greeting.kt` (runtime) | your annotation declaration |
| `ExampleSymbolProcessor` / `Provider` | `<Name>SymbolProcessor` / `<Name>SymbolProcessorProvider` |
| `options/ExampleOptions.kt` | `options/<Name>Options.kt` |
| `core/error/ExampleException.kt` | `core/error/<Name>Exception.kt` |
| `core/common/GreetingSourceAnnotation.kt` | `core/common/<YourAnnotation>SourceAnnotation.kt` |
| `core/greetingFun/GreetingFun.kt` | `core/<yourFamily>Fun/…` |
| `feature/greeting/ProcessGreeting.kt` | `feature/<yourAnnotation>/Process<YourAnnotation>.kt` |

Keep the file **count** even if you rename everything: the root package must end up with exactly
three files.

## Runtime API conventions

The runtime module holds **declarations only** so it can target every Kotlin platform.

- `@Retention(SOURCE)` on every processor-only annotation, without exception. Half-and-half is the
  common drift and it leaks annotations into consumers' bytecode for no benefit.
- Give every generation annotation the **same trailing surface in the same order** (`kdoc`,
  `visibility`, `funName`, …), always with defaults, so a new parameter never changes existing
  behaviour.
- Name tokens are `const val String` sentinels (`"{{project:SimpleName}}"`), not enums, because an
  annotation argument must be a compile-time constant and users will want to concatenate
  (`Token.Prefix + "Foo"`). Substitution happens in the processor.
- Nest auxiliary markers inside their parent (`@Greeting.Exclude`) rather than sharing one top-level
  marker: `@Target` can then be narrowed per parent, and `@Parent.Marker` self-documents at the use
  site. An annotation only ever used as a value takes an empty `@Target()`.
- `explicitApi()` plus a `@RequiresOptIn` marker for anything internal-but-public.

## Generation: string append, not KotlinPoet

`core` generators are `Appendable` extensions named `appendXxx` that append strings. This stays
readable as long as the output is simple — functions, properties, straightforward signatures. If you
find yourself assembling complex generic types or resolving imports by hand, that is the signal to
reconsider KotlinPoet for generation. (KotlinPoet is still used in **tests**, to build inputs.)

Two rules make the string approach safe:

1. **One write-out point.** `createNewKotlinFile` owns the `package` line and the import boilerplate.
   Every feature goes through it, so that boilerplate exists in exactly one place.
2. **Transactional writes.** The block writes into an in-memory buffer first; if it produced nothing,
   no file is opened. An empty `package` + `import` file still lands in the compilation and is worse
   than no file at all.

Keep `Dependencies(aggregating = true, containingFile)` for any processor that reads across
declarations, and push every generated identifier through an escaping helper.

## Per-annotation differences without `when`

`GenerateSourceAnnotation` (in `core/common/`) identifies which annotation triggered a generation and
carries that annotation's rules. It is **deliberately not `sealed`**: rules resolve by polymorphic
dispatch, so adding an annotation means adding one implementation file and overriding only what
differs — no existing branch is edited, and an implementation defined elsewhere still plugs in.

Split rules by scope, because getting this wrong is what pushes projects back toward `when`:

- **Once per generated function** (skip an `object` target, warn about an ineffective marker) →
  a member on the interface, with a "does nothing special" default.
- **Once per property/parameter** (is this excluded, which source property maps here) → a standalone
  function type that the generator takes as an **ordinary parameter**. A test, or any caller not
  driving generation from an annotation, then passes a plain lambda.

## Options

One data class, one parse function, both in `options/`. Two details matter:

- **Parse lazily, inside `process()`.** Parsing in the processor's constructor turns a bad option
  value into a KSP INTERNAL_ERROR with no useful message. The example caches the parsed value in a
  backing field on first successful parse.
- **Expose a `properties` list.** Tests iterate it to render options as `ksp { arg(...) }` and to
  build the option matrix, so adding an option is picked up automatically instead of being forgotten
  in three places.

Be lenient where leniency is safe (a boolean where only `"true"` enables) and strict where a typo
would silently change output (an enum, which reports the valid values).

## Diagnostics

**User misuse is never thrown.** Call `logger.error(message, ksNode)` and `return` immediately.
Throwing surfaces as an opaque INTERNAL_ERROR and can leave a half-written file behind; reporting
gives a clean COMPILATION_ERROR pointing at the user's source. Reserve exceptions for genuinely
unexpected internal states.

The exception hierarchy is an abstract base plus a usage branch and an option branch, with a separate
`Unknown` whose message automatically appends a "report this at <issues URL>" block. It lives in
`core/error/`, which imports only `util` — that leaf status is what lets `options` throw from it
without creating a cycle.

**Every diagnostic carries a solution**, not just a description of what is wrong. The base class
formats a `Solution:` section, so the only discipline required is passing one.

## Adding your second annotation

1. Declare it in the runtime module.
2. Add `<Name>SourceAnnotation` in `core/common/`, overriding only what differs.
3. Add `feature/<name>/Process<Name>.kt`.
4. Reuse the `core` generators; extend `core` if something is missing — never put generation logic in
   `feature`.
5. Add one dispatch line in `<Name>SymbolProcessor.process()`.
6. Add the five feature tests plus `test/` module data.

If step 4 tempts you to import another feature, that shared code belongs in `core`. The Konsist
feature spec will fail the build either way.
