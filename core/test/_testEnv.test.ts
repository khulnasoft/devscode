describe("Test environment", () => {
  test("should have DEVSCODE_GLOBAL_DIR env var set to .devscode-test", () => {
    expect(process.env.DEVSCODE_GLOBAL_DIR).toBeDefined();
    expect(process.env.DEVSCODE_GLOBAL_DIR)?.toMatch(/\.devscode-test$/);
  });
});
