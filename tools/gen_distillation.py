def generate(ctx):
    name = "distillation_pattern_encoder"
    ctx.cube_bottom_top(name, "distillation.encoder.face", "knowledge.inscriber.bottom", "knowledge.inscriber.side")
    ctx.simple_state(name)
    ctx.block_item(name)
    ctx.self_drop(name)
    ctx.mineable("pickaxe", name)
