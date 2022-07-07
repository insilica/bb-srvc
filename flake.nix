{
  description = "A clj-nix flake";

  inputs = {
    nixpkgs.url = "github:NixOS/nixpkgs/nixos-unstable";
    flake-utils.url = "github:numtide/flake-utils";
    clj-nix = {
      url = "github:jlesquembre/clj-nix";
      inputs.nixpkgs.follows = "nixpkgs";
    };
  };
  outputs = { self, nixpkgs, flake-utils, clj-nix }:

    flake-utils.lib.eachDefaultSystem (system:
      let
        pkgs = nixpkgs.legacyPackages.${system};
        cljpkgs = clj-nix.packages."${system}";
      in

      {
        packages = {

          clj = cljpkgs.mkCljBin {
            projectSrc = ./.;
            name = "co.insilica/bb-srvc";
            main-ns = "srvc.bb.cli";
            jdkRunner = pkgs.jdk17_headless;
          };

          graalBin = cljpkgs.mkGraalBin {
            cljDrv = self.packages."${system}".clj;
          };

          graalImage =
            let
              graalDrv = self.packages."${system}".graalBin;
            in
            pkgs.dockerTools.buildLayeredImage {
              name = "srvc";
              tag = "latest";
              config = {
                Cmd = "${graalDrv}/bin/${graalDrv.pname}";
              };
            };

        };
      });

}
